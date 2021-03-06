/*
 * Copyright (c) 2016 Kiall Mac Innes <kiall@macinnes.ie>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package ie.macinnes.htsp.messages;

import ie.macinnes.htsp.HtspMessage;
import ie.macinnes.htsp.RequestMessage;

public class SubscribeRequest extends RequestMessage {
    public static final String METHOD = "subscribe";

    static {
        HtspMessage.addMessageRequestType(METHOD, SubscribeRequest.class);

        // Force registration of Additional Response Types
        new MuxpktResponse();
        new QueueStatusResponse();
        new SignalStatusResponse();
        new SubscriptionGraceResponse();
        new SubscriptionStartResponse();
        new SubscriptionStatusResponse();
    }

    protected Long mChannelId;
    protected Long mSubscriptionId;

    public Long getChannelId() {
        return mChannelId;
    }

    public void setChannelId(Long channelId) {
        mChannelId = channelId;
    }

    public Long getSubscriptionId() {
        return mSubscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        mSubscriptionId = subscriptionId;
    }

    @Override
    public HtspMessage toHtspMessage() {
        HtspMessage htspMessage = super.toHtspMessage();

        htspMessage.putString("method", METHOD);

        htspMessage.putLong("channelId", getChannelId());
        htspMessage.putLong("subscriptionId", getSubscriptionId());

        return htspMessage;
    }
}
