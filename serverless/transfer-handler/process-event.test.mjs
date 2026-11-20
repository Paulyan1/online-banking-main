import assert from 'node:assert/strict';
import test from 'node:test';
import { createDynamoNotificationStore } from './dynamo-notification-store.mjs';
import { handleSqsEvent, processTransferRecord } from './process-event.mjs';

const TABLE = 'online-banking-transfer-notifications';
const NOW = new Date('2026-11-20T17:00:00.000Z');

test('firstDelivery_shouldProcessAndWriteNotification', async () => {
    const client = new ConditionalFakeClient();
    const store = createDynamoNotificationStore(client, TABLE);
    const handled = [];

    const result = await processTransferRecord(sqsRecord('msg-1', {
        eventId: 'TXN-20261120-000042',
        eventType: 'TRANSACTION_COMPLETED',
        transactionId: '42',
        reference: 'TXN-20261120-000042',
        initiatedBy: 'user-1',
        sourceAccount: '1000001',
        targetAccount: '1000002',
        amount: '150.00'
    }), {
        store,
        notify: async (notification) => handled.push(notification),
        now: () => NOW
    });

    assert.equal(result.outcome, 'processed');
    assert.equal(result.eventId, 'TXN-20261120-000042');
    assert.equal(handled.length, 1);

    const puts = client.commands.filter((command) => command.input.ConditionExpression?.includes('attribute_not_exists'));
    assert.equal(puts.length, 1);
    assert.equal(puts[0].input.TableName, TABLE);
    assert.equal(puts[0].input.ConditionExpression, 'attribute_not_exists(eventId)');

    const saved = client.items.get('TXN-20261120-000042');
    assert.equal(saved.transactionId, '42');
    assert.equal(saved.reference, 'TXN-20261120-000042');
    assert.equal(saved.userId, 'user-1');
    assert.equal(saved.accountNumber, '1000002');
    assert.equal(saved.sourceAccount, '1000001');
    assert.equal(saved.type, 'TRANSACTION_COMPLETED');
    assert.equal(saved.status, 'PROCESSED');
    assert.equal(saved.createdAt, '2026-11-20T17:00:00.000Z');
    assert.equal(saved.ttl, Math.floor(NOW.getTime() / 1000) + 30 * 24 * 60 * 60);
    assert.equal(saved.amount, undefined);
});

test('duplicateDelivery_shouldSkipWithoutHandlingAgain', async () => {
    const client = new ConditionalFakeClient();
    const store = createDynamoNotificationStore(client, TABLE);
    let notificationsSent = 0;
    const notify = async () => {
        notificationsSent += 1;
    };
    const record = sqsRecord('msg-1', {
        eventId: 'TXN-20261120-000042',
        transactionId: '42',
        initiatedBy: 'user-1',
        targetAccount: '1000002'
    });
    const dependencies = { store, notify, now: () => NOW };

    const first = await processTransferRecord(record, dependencies);
    const duplicate = await processTransferRecord(
        sqsRecord('msg-2', JSON.parse(record.body)),
        dependencies
    );

    assert.equal(first.outcome, 'processed');
    assert.equal(duplicate.outcome, 'skipped');
    assert.equal(notificationsSent, 1);
    assert.equal(client.items.size, 1);
});

test('failedHandling_shouldReleaseClaimAndSurfaceError', async () => {
    const client = new ConditionalFakeClient();
    const store = createDynamoNotificationStore(client, TABLE);
    const record = sqsRecord('msg-9', {
        eventId: 'TXN-failed',
        transactionId: '9',
        initiatedBy: 'user-9'
    });

    await assert.rejects(
        () => processTransferRecord(record, {
            store,
            notify: async () => {
                throw new Error('notification downstream unavailable');
            },
            now: () => NOW
        }),
        /notification downstream unavailable/
    );

    assert.equal(client.items.has('TXN-failed'), false);
    assert.equal(client.deletedKeys.some((key) => key.eventId === 'TXN-failed'), true);

    const retry = await processTransferRecord(record, {
        store,
        notify: async () => {},
        now: () => new Date('2026-11-20T17:10:00.000Z')
    });
    assert.equal(retry.outcome, 'processed');
    assert.equal(client.items.get('TXN-failed').status, 'PROCESSED');
});

test('handler_shouldFailInvocation_whenAMessageFails', async () => {
    const client = new ConditionalFakeClient();
    const store = createDynamoNotificationStore(client, TABLE);
    const event = {
        Records: [
            sqsRecord('msg-ok', { eventId: 'TXN-ok', transactionId: '1', initiatedBy: 'user-1' }),
            sqsRecord('msg-bad', { eventId: 'TXN-bad', transactionId: '2' })
        ]
    };

    await assert.rejects(
        () => handleSqsEvent(event, {
            store,
            notify: async (notification) => {
                if (notification.eventId === 'TXN-bad') {
                    throw new Error('boom');
                }
            },
            now: () => NOW
        }),
        (error) => {
            assert.match(error.message, /Failed to process 1 transfer event/);
            assert.deepEqual(error.failedMessageIds, ['msg-bad']);
            return true;
        }
    );

    assert.equal(client.items.get('TXN-ok').status, 'PROCESSED');
    assert.equal(client.items.has('TXN-bad'), false);
});

function sqsRecord(messageId, body) {
    return {
        messageId,
        body: JSON.stringify(body)
    };
}

/**
 * In-memory DynamoDB document client. PutItem honors attribute_not_exists(eventId).
 */
class ConditionalFakeClient {
    constructor() {
        this.items = new Map();
        this.commands = [];
        this.deletedKeys = [];
    }

    async send(command) {
        const input = command.input;
        this.commands.push(command);

        if (input.Item) {
            if (input.ConditionExpression === 'attribute_not_exists(eventId)'
                    && this.items.has(input.Item.eventId)) {
                const error = new Error('The conditional request failed');
                error.name = 'ConditionalCheckFailedException';
                throw error;
            }
            this.items.set(input.Item.eventId, { ...input.Item });
            return {};
        }

        if (input.UpdateExpression) {
            const current = this.items.get(input.Key.eventId);
            if (!current) {
                const error = new Error('The conditional request failed');
                error.name = 'ConditionalCheckFailedException';
                throw error;
            }
            current.status = input.ExpressionAttributeValues[':processed'];
            return {};
        }

        if (input.Key) {
            this.deletedKeys.push(input.Key);
            this.items.delete(input.Key.eventId);
            return {};
        }

        throw new Error('Unsupported command');
    }
}
