const express = require('express');
const admin = require('firebase-admin');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

const serviceAccount = require('./firebase-service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const verifyToken = async (req, res, next) => {
  const idToken = req.headers.authorization?.split('Bearer ')[1];

  if (!idToken) {
    return res.status(401).json({ error: 'No authorization token provided' });
  }

  try {
    const decodedToken = await admin.auth().verifyIdToken(idToken);
    req.user = decodedToken;
    next();
  } catch (error) {
    console.error('Error verifying token:', error);
    return res.status(401).json({ error: 'Invalid authorization token' });
  }
};

app.post('/send-notification', verifyToken, async (req, res) => {
  try {
    const { fcmToken, title, body, data } = req.body;

    if (!fcmToken || !title || !body) {
      return res.status(400).json({ error: 'Missing required fields: fcmToken, title, body' });
    }

    const message = {
      token: fcmToken,
      notification: {
        title: title,
        body: body,
      },
      data: data || {},
      android: {
        priority: 'high',
        notification: {
          channelId: 'questix_notifications',
          priority: 'high',
        },
      },
    };

    const response = await admin.messaging().send(message);
    console.log('Successfully sent message:', response);

    res.json({ success: true, messageId: response });
  } catch (error) {
    console.error('Error sending message:', error);
    res.status(500).json({ error: 'Failed to send notification', details: error.message });
  }
});

app.post('/send-multicast-notification', verifyToken, async (req, res) => {
  try {
    const { fcmTokens, title, body, data } = req.body;

    if (!fcmTokens || !Array.isArray(fcmTokens) || fcmTokens.length === 0) {
      return res.status(400).json({ error: 'Invalid FCM tokens array' });
    }

    if (!title || !body) {
      return res.status(400).json({ error: 'Missing required fields: title, body' });
    }

    const message = {
      tokens: fcmTokens,
      notification: {
        title: title,
        body: body,
      },
      data: data || {},
      android: {
        priority: 'high',
        notification: {
          channelId: 'questix_notifications',
          priority: 'high',
        },
      },
    };

    const response = await admin.messaging().sendMulticast(message);
    console.log('Successfully sent multicast message:', response);

    res.json({
      success: true,
      successCount: response.successCount,
      failureCount: response.failureCount,
      responses: response.responses
    });
  } catch (error) {
    console.error('Error sending multicast message:', error);
    res.status(500).json({ error: 'Failed to send notifications', details: error.message });
  }
});

app.get('/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Questix Notification Server running on port ${PORT}`);
  console.log(`Health check: http://localhost:${PORT}/health`);
  console.log(`Android emulator can access via: http://10.0.2.2:${PORT}/health`);
});

module.exports = app;
