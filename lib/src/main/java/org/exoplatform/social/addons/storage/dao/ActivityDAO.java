/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.addons.storage.dao;

import java.util.List;

import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.ActivityStorageException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 18, 2015  
 */
public interface ActivityDAO extends GenericDAO<Activity, Long> {

  // Add customize methods here
  List<Activity> getUserActivities(Identity owner, long time, boolean isNewer, long offset, long limit) throws ActivityStorageException;
  int getNumberOfUserActivities(Identity ownerIdentity);
  
  List<Activity> getSpaceActivities(Identity owner, long time, boolean isNewer, long offset, long limit) throws ActivityStorageException;
  int getNumberOfSpaceActivities(Identity spaceIdentity);
  
  List<Activity> getActivities(Identity owner, Identity viewer, long offset, long limit) throws ActivityStorageException;

  List<Activity> getActivityFeed(Identity ownerIdentity, int offset, int limit);
  int getNumberOfActivitesOnActivityFeed(Identity ownerIdentity);
  List<Activity> getNewerOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit);
  int getNumberOfNewerOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity);
  List<Activity> getOlderOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit);
  int getNumberOfOlderOnActivityFeed(Identity ownerIdentity, ExoSocialActivity baseActivity);
  
  List<Activity> getUserSpacesActivities(Identity ownerIdentity, int offset, int limit);
  int getNumberOfUserSpacesActivities(Identity ownerIdentity);
  
  List<Activity> getActivitiesOfConnections(Identity ownerIdentity, int offset, int limit);
  int getNumberOfActivitiesOfConnections(Identity ownerIdentity);
  List<Activity> getNewerOnActivitiesOfConnections(Identity ownerIdentity, ExoSocialActivity baseActivity, long limit);
  int getNumberOfNewerOnActivitiesOfConnections(Identity ownerIdentity, ExoSocialActivity baseActivity);
  List<Activity> getOlderOnActivitiesOfConnections(Identity ownerIdentity, ExoSocialActivity baseActivity, int limit);
  int getNumberOfOlderOnActivitiesOfConnections(Identity ownerIdentity, ExoSocialActivity baseActivity);
}
