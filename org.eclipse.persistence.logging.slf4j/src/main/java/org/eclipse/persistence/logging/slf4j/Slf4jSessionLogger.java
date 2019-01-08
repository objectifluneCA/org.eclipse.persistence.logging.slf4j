/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
 package org.eclipse.persistence.logging.slf4j;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.logging.SessionLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This is a wrapper class for SLF4J. It is used when messages need to be logged
 * through SLF4J.
 * </p>
 * <p>
 * Use the follwing configuration for using SLF4J with EclipseLink
 * <code>eclipselink.logging.logger</code> and the value
 * <code>org.eclipse.persistence.logging.Slf4jSessionLogger</code>
 * </p>
 * <p>
 * Use the following categories from EclipseLink 
 * (eclipselink.logging.timestamp, eclipselink.logging.thread, 
 * eclipselink.logging.session, eclipselink.logging.connection 
 * y eclipselink.logging.parameters).
 * <p>
 * Logging categories available are:
 * <p>
 * <ul>
 * <li>org.eclipse.persistence.logging.default
 * <li>org.eclipse.persistence.logging.sql
 * <li>org.eclipse.persistence.logging.transaction
 * <li>org.eclipse.persistence.logging.event
 * <li>org.eclipse.persistence.logging.connection
 * <li>org.eclipse.persistence.logging.query
 * <li>org.eclipse.persistence.logging.cache
 * <li>org.eclipse.persistence.logging.propagation
 * <li>org.eclipse.persistence.logging.sequencing
 * <li>org.eclipse.persistence.logging.ejb
 * <li>org.eclipse.persistence.logging.ejb_or_metadata
 * <li>org.eclipse.persistence.logging.weaver
 * <li>org.eclipse.persistence.logging.properties
 * <li>org.eclipse.persistence.logging.server
 * </ul>
 * </p>
 * <p>
 * Mapping of Java Log Level to SLF4J Log Level:
 * </p>
 * <ul>
 * <li>ALL,FINER,FINEST -> TRACE
 * <li>FINE,CONFIG -> DEBUG
 * <li>INFO -> INFO
 * <li>WARNING -> WARN
 * <li>SEVERE -> ERROR
 * </ul>
 * </p>
 * <p>
 * @see https://gist.github.com/msosvi/1325764/raw/50a882f881c2651f6de10cf661813faca22a480d/Slf4jSessionLogger.java 
 */
public class Slf4jSessionLogger extends AbstractSessionLog {

   public static final String ECLIPSELINK_NAMESPACE = "org.eclipse.persistence.logging";
   
   public static final String DEFAULT_CATEGORY = "default";

   public static final String DEFAULT_ECLIPSELINK_NAMESPACE = ECLIPSELINK_NAMESPACE + "." + DEFAULT_CATEGORY;

   private static Map<Integer, LogLevel> MAP_LEVELS = new HashMap<Integer, LogLevel>();
   static {
      MAP_LEVELS.put(SessionLog.ALL, LogLevel.TRACE);
      MAP_LEVELS.put(SessionLog.FINEST, LogLevel.TRACE);
      MAP_LEVELS.put(SessionLog.FINER, LogLevel.TRACE);
      MAP_LEVELS.put(SessionLog.FINE, LogLevel.DEBUG);
      MAP_LEVELS.put(SessionLog.CONFIG, LogLevel.DEBUG);
      MAP_LEVELS.put(SessionLog.INFO, LogLevel.INFO);
      MAP_LEVELS.put(SessionLog.WARNING, LogLevel.WARN);
      MAP_LEVELS.put(SessionLog.SEVERE, LogLevel.ERROR);
   }
   
   private Map<String, Logger> categoryLoggers = new HashMap<String, Logger>();

   public Slf4jSessionLogger() {
      super();
      createCategoryLoggers();
   }

   @Override
   public void log(SessionLogEntry entry) {
      if (!shouldLog(entry.getLevel(), entry.getNameSpace())) {
         return;
      }

      Logger logger = getLogger(entry.getNameSpace());
      LogLevel logLevel = getLogLevel(entry.getLevel());

      StringBuilder message = new StringBuilder();

      message.append(getSupplementDetailString(entry));
      message.append(formatMessage(entry));

      switch (logLevel) {
      case TRACE:
         logger.trace(message.toString());
         break;
      case DEBUG:
         logger.debug(message.toString());
         break;
      case INFO:
         logger.info(message.toString());
         break;
      case WARN:
         logger.warn(message.toString());
         break;
      case ERROR:
         logger.error(message.toString());
         break;
      }
   }

   @Override
   public boolean shouldLog(int level, String category) {
      Logger logger = getLogger(category);
      LogLevel logLevel = getLogLevel(level);
      
      switch (logLevel) {
      case TRACE:
         return logger.isTraceEnabled();
      case DEBUG:
         return logger.isDebugEnabled();
      case INFO:
         return logger.isInfoEnabled();
      case WARN:
         return logger.isWarnEnabled();
      case ERROR:
         return logger.isErrorEnabled();
      default:
         return true;
      }
   }

   @Override
   public boolean shouldLog(int level) {
      return shouldLog(level, "default");
   }

   /**
    * Return true if SQL logging should log visible bind parameters. If the
    * shouldDisplayData is not set, return false.
    */
   @Override
   public boolean shouldDisplayData() {
      if (this.shouldDisplayData != null) {
         return shouldDisplayData.booleanValue();
      } else {
         return false;
      }
   }

   /**
    * Initialize loggers eagerly
    */
   private void createCategoryLoggers() {
      for (String category : SessionLog.loggerCatagories) {
         addLogger(category, ECLIPSELINK_NAMESPACE + "." + category);
      }
      // Logger default para cuando no hay categoría.
      addLogger(DEFAULT_CATEGORY, DEFAULT_ECLIPSELINK_NAMESPACE);
   }

   /**
    * INTERNAL: Add Logger to the categoryLoggers.
    */
   private void addLogger(String loggerCategory, String loggerNameSpace) {
      categoryLoggers.put(loggerCategory,
            LoggerFactory.getLogger(loggerNameSpace));
   }

   /**
    * INTERNAL: Return the Logger for the given category
    */
   private Logger getLogger(String category) {
      if (!hasText(category)
            || !this.categoryLoggers.containsKey(category)) {
         category = DEFAULT_CATEGORY;
      }

      return categoryLoggers.get(category);

   }

   /**
    * Return the corresponding Slf4j Level for a given EclipseLink level.
    */
   private LogLevel getLogLevel(Integer level) {
      LogLevel logLevel = MAP_LEVELS.get(level);

      if (logLevel == null)
         logLevel = LogLevel.OFF;

      return logLevel;
   }

   /**
    * SLF4J log levels.
    */
   enum LogLevel {
      TRACE, DEBUG, INFO, WARN, ERROR, OFF
   }
   
   public static boolean hasText(String str) {
      return str != null;
  }
   
   

}