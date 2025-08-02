package com.example.aeronproducer.service;

import io.aeron.Publication;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;

@Service
public class MessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducer.class);

    @Autowired
    private Publication publication;

    public boolean sendMessage(String message) {
        try {
            byte[] messageBytes = message.getBytes();
            UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocate(messageBytes.length));
            buffer.putBytes(0, messageBytes);

            long result = publication.offer(buffer, 0, messageBytes.length);

            if (result > 0) {
                logger.info("Message sent successfully: {}", message);
                return true;
            } else if (result == Publication.BACK_PRESSURED) {
                logger.warn("Back pressure detected, message not sent: {}", message);
                return false;
            } else if (result == Publication.NOT_CONNECTED) {
                logger.error("Publication not connected, message not sent: {}", message);
                return false;
            } else if (result == Publication.ADMIN_ACTION) {
                logger.warn("Admin action required, message not sent: {}", message);
                return false;
            } else if (result == Publication.CLOSED) {
                logger.error("Publication closed, message not sent: {}", message);
                return false;
            } else {
                logger.error("Unknown error sending message: {}", message);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error sending message: {}", message, e);
            return false;
        }
    }
}