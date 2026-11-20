import { DeleteCommand, PutCommand, UpdateCommand } from '@aws-sdk/lib-dynamodb';

/**
 * DynamoDB adapter for transfer notification records.
 * claim() is the idempotency gate: the condition fails when eventId already exists,
 * including when two Lambda invocations race on the same delivery.
 */
export function createDynamoNotificationStore(docClient, tableName) {
    return {
        async claim(item) {
            try {
                await docClient.send(new PutCommand({
                    TableName: tableName,
                    Item: item,
                    ConditionExpression: 'attribute_not_exists(eventId)'
                }));
                return { claimed: true };
            } catch (error) {
                if (error.name === 'ConditionalCheckFailedException') {
                    return { claimed: false };
                }
                throw error;
            }
        },

        async markProcessed(eventId) {
            await docClient.send(new UpdateCommand({
                TableName: tableName,
                Key: { eventId },
                UpdateExpression: 'SET #status = :processed',
                ConditionExpression: 'attribute_exists(eventId)',
                ExpressionAttributeNames: { '#status': 'status' },
                ExpressionAttributeValues: { ':processed': 'PROCESSED' }
            }));
        },

        async release(eventId) {
            await docClient.send(new DeleteCommand({
                TableName: tableName,
                Key: { eventId }
            }));
        }
    };
}
