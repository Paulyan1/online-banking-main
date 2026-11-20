import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { DynamoDBDocumentClient } from '@aws-sdk/lib-dynamodb';
import { createDynamoNotificationStore } from './dynamo-notification-store.mjs';
import { handleSqsEvent, logTransferNotification } from './process-event.mjs';

const DEFAULT_TABLE = 'online-banking-transfer-notifications';

let store;

export const handler = async (event) => {
    return handleSqsEvent(event, {
        store: notificationStore(),
        notify: logTransferNotification
    });
};

function notificationStore() {
    if (!store) {
        const config = {
            region: process.env.AWS_REGION ?? 'ca-central-1'
        };
        if (process.env.DYNAMODB_ENDPOINT) {
            config.endpoint = process.env.DYNAMODB_ENDPOINT;
        }
        const client = DynamoDBDocumentClient.from(new DynamoDBClient(config));
        const tableName = process.env.TRANSFER_NOTIFICATIONS_TABLE ?? DEFAULT_TABLE;
        store = createDynamoNotificationStore(client, tableName);
    }
    return store;
}
