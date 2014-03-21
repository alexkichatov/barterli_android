/*******************************************************************************
 * Copyright 2014, barter.li
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package li.barter.http.rabbitmq;

import com.rabbitmq.client.QueueingConsumer;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.Map;

/**
 * Consumes messages from a RabbitMQ broker
 */
public class ChatRabbitMQConnector extends AbstractRabbitMQConnector {

    private static final String TAG = "ChatRabbitMQConnector";

    public ChatRabbitMQConnector(final String server, final int port, final String virtualHost, final String exchange, final ExchangeType exchangeType) {
        super(server, port, virtualHost, exchange, exchangeType);
    }

    // The Queue name for this consumer
    private String           mQueue;
    private QueueingConsumer mSubscription;

    // last message to post back
    private byte[]           mLastMessage;

    // An interface to be implemented by an object that is interested in
    // messages(listener)
    public interface OnReceiveMessageHandler {
        public void onReceiveMessage(byte[] message);
    };

    // A reference to the listener, we can only have one at a time(for now)
    private OnReceiveMessageHandler mOnReceiveMessageHandler;

    private final Handler           mHandler       = new Handler();

    // Create runnable for posting back to main thread
    final Runnable                  mReturnMessage = new Runnable() {
                                                       @Override
                                                       public void run() {
                                                           mOnReceiveMessageHandler
                                                                           .onReceiveMessage(mLastMessage);
                                                       }
                                                   };

    final Runnable                  mConsumeRunner = new Runnable() {
                                                       @Override
                                                       public void run() {
                                                           consume();
                                                       }
                                                   };

    /**
     * Create Exchange and then start consuming. A binding needs to be added
     * before any messages will be delivered
     */
    public boolean connectToRabbitMQ(final String queueName,
                    final boolean durable, final boolean exclusive,
                    final boolean autoDelete, final Map<String, Object> args) {
        if (super.connectToRabbitMQ("barterli", "barter")) {

            try {
                Log.d(TAG, "Connected");
                mQueue = mChannel.queueDeclare(queueName, durable, exclusive,
                                autoDelete, args).getQueue();
                mSubscription = new QueueingConsumer(mChannel);
                mChannel.basicConsume(mQueue, false, mSubscription);
            } catch (final IOException e) {
                e.printStackTrace();
                return false;
            }
            if (mExchangeType == ExchangeType.FANOUT) {
                addBinding("");// fanout has default binding
            }

            setIsRunning(true);
            mHandler.post(mConsumeRunner);

            return true;
        }
        return false;
    }

    /**
     * Add a binding between this consumers Queue and the Exchange with
     * routingKey
     * 
     * @param routingKey the binding key eg GOOG
     */
    public void addBinding(final String routingKey) {
        try {
            mChannel.queueBind(mQueue, mExchange, routingKey);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove binding between this consumers Queue and the Exchange with
     * routingKey
     * 
     * @param routingKey the binding key
     */
    public void removeBinding(final String routingKey) {
        try {
            mChannel.queueUnbind(mQueue, mExchange, routingKey);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the callback for received messages
     * 
     * @param handler The callback
     */
    public void setOnReceiveMessageHandler(final OnReceiveMessageHandler handler) {
        mOnReceiveMessageHandler = handler;
    };

    private void consume() {
        final Thread thread = new Thread() {

            @Override
            public void run() {
                while (isRunning()) {
                    QueueingConsumer.Delivery delivery;
                    try {
                        delivery = mSubscription.nextDelivery();
                        mLastMessage = delivery.getBody();
                        mHandler.post(mReturnMessage);
                        try {
                            mChannel.basicAck(delivery.getEnvelope()
                                            .getDeliveryTag(), false);
                        } catch (final IOException e) {
                            e.printStackTrace();
                        }
                    } catch (final InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        };
        thread.start();

    }

    /**
     * Publish a message to this consumer's queue
     * 
     * @param routingKey The binding key
     * @param message The message to publish
     */
    public void publish(final String routingKey, final String message) {
        publish(mQueue, routingKey, message);
    }

}
