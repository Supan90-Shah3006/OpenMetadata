/*
 *  Copyright 2021 Collate
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openmetadata.service.util;

import static org.openmetadata.service.events.subscription.AlertsRuleEvaluator.getEntity;
import static org.openmetadata.service.formatter.util.FormatterUtil.getFormattedMessages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.openmetadata.schema.EntityInterface;
import org.openmetadata.schema.entity.feed.Thread;
import org.openmetadata.schema.type.ChangeDescription;
import org.openmetadata.schema.type.ChangeEvent;
import org.openmetadata.service.Entity;
import org.openmetadata.service.formatter.decorators.FeedMessageDecorator;
import org.openmetadata.service.formatter.decorators.MessageDecorator;
import org.openmetadata.service.formatter.util.FeedMessage;
import org.openmetadata.service.resources.feeds.MessageParser;

@Slf4j
public final class FeedUtils {
  private FeedUtils() {}

  public static List<Thread> getThreadWithMessage(
      ChangeEvent changeEvent, String loggedInUserName) {
    if (changeEvent == null || changeEvent.getEntity() == null) {
      return Collections.emptyList(); // Response has no entity to produce change event from
    }

    // Change Event is of Thread or Data Assets
    if (changeEvent.getEntityType().equals(Entity.THREAD)) {
      // TODO:
      return Collections.emptyList();
      // return List.of(AlertsRuleEvaluator.getThread(changeEvent));
    } else if (Entity.getEntityList().contains(changeEvent.getEntityType())) {
      return populateMessageForDataAssets(changeEvent, loggedInUserName);
    } else {
      LOG.error(
          "Invalid Entity Type: {}, Currently Change Events are expected as Thread or Data Assets",
          changeEvent.getEntityType());
      return Collections.emptyList();
    }
  }

  private static List<Thread> populateMessageForThread(
      ChangeEvent changeEvent, String loggedInUserName) {
    return null;
  }

  private static List<Thread> populateMessageForDataAssets(
      ChangeEvent changeEvent, String loggedInUserName) {
    String message;
    EntityInterface entityInterface = getEntity(changeEvent);
    MessageParser.EntityLink about =
        new MessageParser.EntityLink(
            changeEvent.getEntityType(), entityInterface.getFullyQualifiedName(), null, null, null);
    // In Case EventType is not valid
    return switch (changeEvent.getEventType()) {
      case ENTITY_CREATED -> {
        message =
            String.format(
                "Created **%s**: `%s`",
                changeEvent.getEntityType(), entityInterface.getFullyQualifiedName());
        yield List.of(getThread(about.getLinkString(), message, loggedInUserName));
      }
      case ENTITY_UPDATED -> getThreadWithMessage(
          entityInterface, changeEvent.getChangeDescription(), loggedInUserName);
      case ENTITY_SOFT_DELETED -> {
        message =
            String.format(
                "Soft deleted **%s**: `%s`",
                changeEvent.getEntityType(), entityInterface.getFullyQualifiedName());
        yield List.of(getThread(about.getLinkString(), message, loggedInUserName));
      }
      case ENTITY_DELETED -> {
        message =
            String.format(
                "Permanently Deleted **%s**: `%s`",
                changeEvent.getEntityType(), entityInterface.getFullyQualifiedName());
        yield List.of(getThread(about.getLinkString(), message, loggedInUserName));
      }
      case LOGICAL_TEST_CASE_ADDED -> {
        message =
            String.format(
                "Added Logical Test Cases to **%s**: `%s`",
                changeEvent.getEntityType(), entityInterface.getFullyQualifiedName());
        yield List.of(getThread(about.getLinkString(), message, loggedInUserName));
      }
      default -> {
        if (entityInterface.getChangeDescription() == null) {
          yield Collections.emptyList();
        }
        yield getThreadWithMessage(
            entityInterface, entityInterface.getChangeDescription(), loggedInUserName);
      }
    };
  }

  private static List<Thread> getThreadWithMessage(
      EntityInterface entity, ChangeDescription changeDescription, String loggedInUserName) {
    List<Thread> threads = new ArrayList<>();

    MessageDecorator<FeedMessage> feedFormatter = new FeedMessageDecorator();
    Map<MessageParser.EntityLink, String> messages =
        getFormattedMessages(feedFormatter, changeDescription, entity);

    // Create an automated thread
    for (Map.Entry<MessageParser.EntityLink, String> entry : messages.entrySet()) {
      threads.add(getThread(entry.getKey().getLinkString(), entry.getValue(), loggedInUserName));
    }

    return threads;
  }

  private static Thread getThread(String linkString, String message, String loggedInUserName) {
    return new Thread()
        .withId(UUID.randomUUID())
        .withThreadTs(System.currentTimeMillis())
        .withCreatedBy(loggedInUserName)
        .withAbout(linkString)
        .withReactions(Collections.emptyList())
        .withUpdatedBy(loggedInUserName)
        .withUpdatedAt(System.currentTimeMillis())
        .withMessage(message);
  }
}
