const THIRTY_DAYS_IN_SECONDS = 30 * 24 * 60 * 60;

/**
 * Idempotency key for one transfer notification.
 * Prefer the event id published with the message so a second SQS delivery,
 * or a republish of the same transfer, collapses to one record.
 * Fall back to the SQS message id, which stays stable across redrives of that message.
 */
export function resolveEventId(record, body) {
    const fromBody = body?.eventId;
    if (typeof fromBody === 'string' && fromBody.trim() !== '') {
        return fromBody.trim();
    }
    if (typeof record?.messageId === 'string' && record.messageId.trim() !== '') {
        return record.messageId.trim();
    }
    throw new Error('Transfer event is missing an event id');
}

export function buildNotification(record, body, now) {
    const eventId = resolveEventId(record, body);
    const notification = {
        eventId,
        transactionId: text(body.transactionId),
        reference: text(body.reference) ?? text(body.eventId),
        userId: text(body.initiatedBy) ?? text(body.userId),
        accountNumber: text(body.targetAccount) ?? text(body.accountNumber),
        sourceAccount: text(body.sourceAccount),
        type: text(body.eventType) ?? 'TRANSACTION_COMPLETED',
        status: 'PROCESSING',
        createdAt: now.toISOString(),
        ttl: Math.floor(now.getTime() / 1000) + THIRTY_DAYS_IN_SECONDS
    };
    return dropEmpty(notification);
}

/**
 * Claim the event with a conditional write, handle it once, then mark it processed.
 * A lost claim means another delivery already owns the event, so this one is skipped.
 * A handling failure releases the claim and rethrows so SQS can retry and then DLQ the message.
 */
export async function processTransferRecord(record, { store, notify, now = () => new Date() }) {
    const body = parseBody(record);
    const notification = buildNotification(record, body, now());
    const claim = await store.claim(notification);

    if (!claim.claimed) {
        return { eventId: notification.eventId, outcome: 'skipped' };
    }

    try {
        await notify(notification, body);
    } catch (error) {
        await releaseClaim(store, notification.eventId, error);
        throw error;
    }

    await store.markProcessed(notification.eventId);
    return { eventId: notification.eventId, outcome: 'processed' };
}

export async function handleSqsEvent(event, dependencies) {
    const records = event?.Records ?? [];
    let processedCount = 0;
    let skippedCount = 0;
    const failedMessageIds = [];

    for (const record of records) {
        try {
            const result = await processTransferRecord(record, dependencies);
            if (result.outcome === 'skipped') {
                skippedCount += 1;
                console.log(JSON.stringify({
                    message: 'Duplicate transfer event skipped',
                    eventId: result.eventId,
                    sqsMessageId: record.messageId
                }));
            } else {
                processedCount += 1;
            }
        } catch (error) {
            failedMessageIds.push(record.messageId ?? 'unknown');
            console.error(JSON.stringify({
                message: 'Transfer event failed',
                sqsMessageId: record.messageId ?? null,
                error: error.message
            }));
        }
    }

    if (failedMessageIds.length > 0) {
        const failure = new Error(
            `Failed to process ${failedMessageIds.length} transfer event(s): ${failedMessageIds.join(', ')}`
        );
        failure.failedMessageIds = failedMessageIds;
        throw failure;
    }

    return {
        processedCount,
        skippedCount,
        status: 'success'
    };
}

export function logTransferNotification(notification) {
    console.log(JSON.stringify({
        message: 'Transfer notification processed',
        eventId: notification.eventId,
        transactionId: notification.transactionId ?? null,
        reference: notification.reference ?? null,
        userId: notification.userId ?? null,
        accountNumber: notification.accountNumber ?? null,
        type: notification.type,
        status: 'PROCESSED'
    }));
}

function parseBody(record) {
    try {
        const body = JSON.parse(record.body);
        if (body === null || typeof body !== 'object' || Array.isArray(body)) {
            throw new Error('Transfer event body must be a JSON object');
        }
        return body;
    } catch (error) {
        if (error instanceof SyntaxError) {
            throw new Error('Transfer event body is not valid JSON');
        }
        throw error;
    }
}

async function releaseClaim(store, eventId, originalError) {
    try {
        await store.release(eventId);
    } catch (releaseError) {
        console.error(JSON.stringify({
            message: 'Failed to release idempotency claim',
            eventId,
            error: releaseError.message,
            cause: originalError.message
        }));
    }
}

function text(value) {
    if (value === undefined || value === null) {
        return null;
    }
    const rendered = String(value).trim();
    return rendered === '' ? null : rendered;
}

function dropEmpty(item) {
    return Object.fromEntries(
        Object.entries(item).filter(([, value]) => value !== null && value !== undefined)
    );
}
