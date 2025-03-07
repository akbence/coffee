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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import hu.icellmobilsoft.coffee.dto.common.LogConstants;
import hu.icellmobilsoft.coffee.dto.exception.BaseException;
import hu.icellmobilsoft.coffee.module.redisstream.config.IRedisStreamConstant;
import hu.icellmobilsoft.coffee.se.logging.Logger;
import hu.icellmobilsoft.coffee.se.logging.mdc.MDC;
import redis.clients.jedis.StreamEntry;

/**
 * Basic Redis Stream consumer tools
 * 
 * @author imre.scheffer
 * @since 1.5.0
 */
public class BaseStreamConsumer {

    @Inject
    private Logger log;

    @Resource(lookup = "java:app/AppName")
    private String applicationName;

    /**
     * Logging MDC handling, setting variables
     * 
     * @param streamEntry
     *            {@link IRedisStreamConsumer#onStream(StreamEntry)}
     */
    protected void handleMDC(StreamEntry streamEntry) {
        MDC.put(LogConstants.LOG_SERVICE_NAME, applicationName);

        Map<String, String> fieldMap = streamEntry.getFields();
        String mainData = fieldMap.get(IRedisStreamConstant.Common.DATA_KEY_MESSAGE);
        String flowId = fieldMap.getOrDefault(IRedisStreamConstant.Common.DATA_KEY_FLOW_ID, mainData);
        MDC.put(LogConstants.LOG_SESSION_ID, flowId);
    }

    /**
     * Another stream message handlings, can be setup in every consumer implementation
     * 
     * @param streamEntry
     *            {@link IRedisStreamConsumer#onStream(StreamEntry)}
     * @return true - skip message processing, dont send the message to business process but send ACK.
     * @throws BaseException
     *             technical error on processing
     */
    protected boolean customize(StreamEntry streamEntry) throws BaseException {
        return isExpiredTtl(streamEntry);
    }

    /**
     * Message TTL is expired?
     * 
     * @param streamEntry
     *            {@link IRedisStreamConsumer#onStream(StreamEntry)}
     * @return true - expires processing allowed time
     * @throws BaseException
     *             technical error on processing
     */
    protected boolean isExpiredTtl(StreamEntry streamEntry) throws BaseException {
        Map<String, String> fieldMap = streamEntry.getFields();
        String ttl = fieldMap.get(IRedisStreamConstant.Common.DATA_KEY_TTL);
        if (StringUtils.isNotBlank(ttl)) {
            try {
                LocalDateTime ttlDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(ttl)), ZoneId.systemDefault());
                if (ttlDate.isBefore(LocalDateTime.now())) {
                    log.trace("Message [{0}] with ttl [{1}] exceeded.", streamEntry.getID(), ttl);
                    return true;
                }
            } catch (NumberFormatException e) {
                log.trace("Cant parse message [{0}] with ttl [{1}] value, skipping ttl check. Error: [{2}]", streamEntry.getID(), ttl,
                        e.getLocalizedMessage());
            }
        }
        return false;
    }

}
