const { onDocumentCreated, onDocumentUpdated } = require('firebase-functions/v2/firestore');
const admin = require('firebase-admin');

admin.initializeApp();

exports.oneventcreated = onDocumentCreated('events/{eventId}', async (event) => {
    const data = event.data.data();

    const message = {
        notification: {
            title: 'New NSS Event: ' + data.title,
            body: `A new event has been posted for ${new Date(data.date).toLocaleDateString()}. Check it out!`
        },
        topic: 'events_updates'
    };

    return admin.messaging().send(message);
});

exports.onpenaltyapplied = onDocumentUpdated('events/{eventId}', async (event) => {
    const newData = event.data.after.data();
    const oldData = event.data.before.data();

    if (newData.isPenaltyApplied && !oldData.isPenaltyApplied) {
        const message = {
            notification: {
                title: 'Attendance Updated',
                body: `Penalties have been processed for the event: ${newData.title}.`
            },
            topic: 'events_updates'
        };
        return admin.messaging().send(message);
    }
});
