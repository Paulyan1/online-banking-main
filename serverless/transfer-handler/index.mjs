export const handler = async (event) => {
    const records = event.Records ?? [];

    for (const record of records) {
        const transferEvent = JSON.parse(record.body);

        console.log(JSON.stringify({
            message: "Transfer event received",
            sqsMessageId: record.messageId,
            eventType: transferEvent.eventType ?? "unknown"
        }));
    }

    return {
        processedCount: records.length,
        status: "success"
    };
};
