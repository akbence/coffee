/*-
 * #%L
 * Coffee
 * %%
 * Copyright (C) 2020 - 2021 i-Cell Mobilsoft Zrt.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package hu.icellmobilsoft.coffee.module.redisstream.consumer;

import java.util.Collections;
import java.util.Map;

import hu.icellmobilsoft.coffee.dto.exception.BaseException;
import hu.icellmobilsoft.coffee.module.redisstream.config.IRedisStreamConstant;
import hu.icellmobilsoft.coffee.se.logging.mdc.MDC;
import redis.clients.jedis.StreamEntry;

/**
 * Default Redis stream consumer for PIPE consumer
 * 
 * @author imre.scheffer
 * @since 1.5.0
 */
public abstract class AbstractStreamPipeConsumer extends BaseStreamConsumer implements IRedisStreamPipeConsumer {
    /**
     * {@inheritDoc}
     * 
     * <br>
     * Input is full Redis Stream message
     */
    @Override
    public Map<String, Object> onStream(StreamEntry streamEntry) throws BaseException {
        try {
            handleMDC(streamEntry);

            if (customize(streamEntry)) {
                return Collections.emptyMap();
            }
            String mainData = streamEntry.getFields().get(IRedisStreamConstant.Common.DATA_KEY_MESSAGE);
            return doWork(mainData);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Default imlepmenting menthod, contains only business logic
     * 
     * @param text
     *            stream data content, {@value IRedisStreamConstant.Common#DATA_KEY_MESSAGE} key value, which can be string or json
     * @return result data which can be used after the request scope destroying, used in {@code IRedisStreamPipeConsumer#afterAck(StreamEntry, Map)}
     * @throws BaseException
     *             technical error on processing
     */
    public abstract Map<String, Object> doWork(String text) throws BaseException;
}
