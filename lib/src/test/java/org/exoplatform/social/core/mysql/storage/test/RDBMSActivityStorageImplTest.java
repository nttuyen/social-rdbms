/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.core.mysql.storage.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.mysql.test.AbstractCoreTest;
import org.exoplatform.social.core.mysql.test.MaxQueryNumber;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.mysql.test.QueryNumberTest;

@QueryNumberTest
public class RDBMSActivityStorageImplTest extends AbstractCoreTest {
  
  private IdentityStorage identityStorage;
  private ActivityStorage activityStorage;
  
  private List<ExoSocialActivity> tearDownActivityList;
  private List<Space> tearDownSpaceList;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    identityStorage = getService(IdentityStorage.class);
    activityStorage = getService(ActivityStorage.class);
    
    assertNotNull(identityStorage);
    assertNotNull(activityStorage);

    assertNotNull(rootIdentity.getId());
    assertNotNull(johnIdentity.getId());
    assertNotNull(maryIdentity.getId());
    assertNotNull(demoIdentity.getId());

    tearDownActivityList = new ArrayList<ExoSocialActivity>();
    tearDownSpaceList = new ArrayList<Space>();
  }

  @Override
  protected void tearDown() throws Exception {
    for (ExoSocialActivity activity : tearDownActivityList) {
      activityStorageImpl.deleteActivity(activity.getId());
    }
    identityStorage.deleteIdentity(rootIdentity);
    identityStorage.deleteIdentity(johnIdentity);
    identityStorage.deleteIdentity(maryIdentity);
    identityStorage.deleteIdentity(demoIdentity);
    //
    for (Space space : tearDownSpaceList) {
      Identity spaceIdentity = identityStorage.findIdentity(SpaceIdentityProvider.NAME, space.getPrettyName());
      if (spaceIdentity != null) {
        identityStorage.deleteIdentity(spaceIdentity);
      }
      spaceService.deleteSpace(space);
    }
    super.tearDown();
  }
  
  @MaxQueryNumber(516)
  public void testSaveActivity() {
    
    ExoSocialActivity activity = createActivity(0);
    //
    activityStorageImpl.saveActivity(demoIdentity, activity);
    
    assertNotNull(activity.getId());
    
    ExoSocialActivity rs = activityStorageImpl.getActivity(activity.getId());
    
    //
    assertTrue(Arrays.asList(rs.getLikeIdentityIds()).contains("demo"));
    
    //
    tearDownActivityList.add(activity);
    
  }
  @MaxQueryNumber(516)
  public void testUpdateActivity() {
    ExoSocialActivity activity = createActivity(1);
    //
    activityStorageImpl.saveActivity(demoIdentity, activity);
    
    activity.setTitle("Title after updated");
    
    //update
    activityStorageImpl.updateActivity(activity);
    
    ExoSocialActivity res = activityStorageImpl.getActivity(activity.getId());
    
    assertEquals("Title after updated", res.getTitle());
    //
    tearDownActivityList.add(activity);
  }
  @MaxQueryNumber(516)
  public void testGetActivity() {
    ExoSocialActivity activity = createActivity(1);
    //
    activityStorageImpl.saveActivity(demoIdentity, activity);
    
    
  }
  @MaxQueryNumber(530)
  public void testGetNewerOnUserActivities() {
    createActivities(2, demoIdentity);
    ExoSocialActivity firstActivity = activityStorageImpl.getUserActivities(demoIdentity, 0, 10).get(0);
    assertEquals(0, activityStorageImpl.getNewerOnUserActivities(demoIdentity, firstActivity, 10).size());
    assertEquals(0, activityStorageImpl.getNumberOfNewerOnUserActivities(demoIdentity, firstActivity));
    //
    createActivities(2, maryIdentity);
    assertEquals(0, activityStorageImpl.getNewerOnUserActivities(demoIdentity, firstActivity, 10).size());
    assertEquals(0, activityStorageImpl.getNumberOfNewerOnUserActivities(demoIdentity, firstActivity));
    //
    createActivities(2, demoIdentity);
    assertEquals(2, activityStorageImpl.getNewerOnUserActivities(demoIdentity, firstActivity, 10).size());
    assertEquals(2, activityStorageImpl.getNumberOfNewerOnUserActivities(demoIdentity, firstActivity));
  }
  @MaxQueryNumber(532)
  public void testGetOlderOnUserActivities() {
    createActivities(2, demoIdentity);
    ExoSocialActivity baseActivity = activityStorageImpl.getUserActivities(demoIdentity, 0, 10).get(0);
    assertEquals(1, activityStorageImpl.getOlderOnUserActivities(demoIdentity, baseActivity, 10).size());
    assertEquals(1, activityStorageImpl.getNumberOfOlderOnUserActivities(demoIdentity, baseActivity));
    //
    createActivities(2, maryIdentity);
    assertEquals(1, activityStorageImpl.getOlderOnUserActivities(demoIdentity, baseActivity, 10).size());
    assertEquals(1, activityStorageImpl.getNumberOfOlderOnUserActivities(demoIdentity, baseActivity));
    //
    createActivities(2, demoIdentity);
    baseActivity = activityStorageImpl.getUserActivities(demoIdentity, 0, 10).get(0);
    assertEquals(3, activityStorageImpl.getOlderOnUserActivities(demoIdentity, baseActivity, 10).size());
    assertEquals(3, activityStorageImpl.getNumberOfOlderOnUserActivities(demoIdentity, baseActivity));
  }
  @MaxQueryNumber(695)
  public void testGetNewerOnActivityFeed() {
    createActivities(3, demoIdentity);
    ExoSocialActivity demoBaseActivity = activityStorageImpl.getActivityFeed(demoIdentity, 0, 10).get(0);
    assertEquals(0, activityStorageImpl.getNewerOnActivityFeed(demoIdentity, demoBaseActivity, 10).size());
    assertEquals(0, activityStorageImpl.getNumberOfNewerOnActivityFeed(demoIdentity, demoBaseActivity));
    //
    createActivities(1, demoIdentity);
    assertEquals(1, activityStorageImpl.getNewerOnActivityFeed(demoIdentity, demoBaseActivity, 10).size());
    assertEquals(1, activityStorageImpl.getNumberOfNewerOnActivityFeed(demoIdentity, demoBaseActivity));
    //
    createActivities(2, maryIdentity);
    Relationship demoMaryConnection = relationshipManager.inviteToConnect(demoIdentity, maryIdentity);
    relationshipManager.confirm(maryIdentity, demoIdentity);
    createActivities(2, maryIdentity);
    assertEquals(5, activityStorageImpl.getNewerOnActivityFeed(demoIdentity, demoBaseActivity, 10).size());
    assertEquals(5, activityStorageImpl.getNumberOfNewerOnActivityFeed(demoIdentity, demoBaseActivity));
    
    //clear data
    relationshipManager.delete(demoMaryConnection);
  }
  @MaxQueryNumber(695)
  public void testGetOlderOnActivityFeed() throws Exception {
    createActivities(3, demoIdentity);
    createActivities(2, maryIdentity);
    Relationship maryDemoConnection = relationshipManager.inviteToConnect(maryIdentity, demoIdentity);
    relationshipManager.confirm(demoIdentity, maryIdentity);
    
    List<ExoSocialActivity> demoActivityFeed = activityStorageImpl.getActivityFeed(demoIdentity, 0, 10);
    ExoSocialActivity baseActivity = demoActivityFeed.get(4);
    assertEquals(0, activityStorageImpl.getNumberOfOlderOnActivityFeed(demoIdentity, baseActivity));
    assertEquals(0, activityStorageImpl.getOlderOnActivityFeed(demoIdentity, baseActivity, 10).size());
    //
    createActivities(1, johnIdentity);
    assertEquals(0, activityStorageImpl.getNumberOfOlderOnActivityFeed(demoIdentity, baseActivity));
    assertEquals(0, activityStorageImpl.getOlderOnActivityFeed(demoIdentity, baseActivity, 10).size());
    //
    baseActivity = demoActivityFeed.get(2);
    assertEquals(2, activityStorageImpl.getNumberOfOlderOnActivityFeed(demoIdentity, baseActivity));
    assertEquals(2, activityStorageImpl.getOlderOnActivityFeed(demoIdentity, baseActivity, 10).size());
    
    //clear data
    relationshipManager.delete(maryDemoConnection);
  }
  @MaxQueryNumber(1129)
  public void testGetNewerOnActivitiesOfConnections() throws Exception {
    List<Relationship> relationships = new ArrayList<Relationship> ();
    createActivities(3, maryIdentity);
    createActivities(1, demoIdentity);
    createActivities(2, johnIdentity);
    createActivities(2, rootIdentity);
    
    List<ExoSocialActivity> maryActivities = activityStorageImpl.getActivitiesOfIdentity(maryIdentity, 0, 10);
    assertEquals(3, maryActivities.size());
    
    //base activity is the first activity created by mary
    ExoSocialActivity baseActivity = maryActivities.get(2);
    
    //As mary has no connections, there are any activity on her connection stream
    assertEquals(0, activityStorageImpl.getNewerOnActivitiesOfConnections(maryIdentity, baseActivity, 10).size());
    assertEquals(0, activityStorageImpl.getNumberOfNewerOnActivitiesOfConnections(maryIdentity, baseActivity));
    
    //demo connect with mary
    Relationship maryDemoRelationship = relationshipManager.inviteToConnect(maryIdentity, demoIdentity);
    relationshipManager.confirm(maryIdentity, demoIdentity);
    relationships.add(maryDemoRelationship);
    
    assertEquals(1, activityStorageImpl.getNewerOnActivitiesOfConnections(maryIdentity, baseActivity, 10).size());
    assertEquals(1, activityStorageImpl.getNumberOfNewerOnActivitiesOfConnections(maryIdentity, baseActivity));
    
    //demo has 2 activities created by mary newer than the base activity
    assertEquals(2, activityStorageImpl.getNewerOnActivitiesOfConnections(demoIdentity, baseActivity, 10).size());
    assertEquals(2, activityStorageImpl.getNumberOfNewerOnActivitiesOfConnections(demoIdentity, baseActivity));
    
    //john connects with mary
    Relationship maryJohnRelationship = relationshipManager.inviteToConnect(maryIdentity, johnIdentity);
    relationshipManager.confirm(maryIdentity, johnIdentity);
    relationships.add(maryJohnRelationship);
    
    assertEquals(3, activityStorageImpl.getNewerOnActivitiesOfConnections(maryIdentity, baseActivity, 10).size());
    assertEquals(3, activityStorageImpl.getNumberOfNewerOnActivitiesOfConnections(maryIdentity, baseActivity));
    
    assertEquals(2, activityStorageImpl.getNewerOnActivitiesOfConnections(johnIdentity, baseActivity, 10).size());
    assertEquals(2, activityStorageImpl.getNumberOfNewerOnActivitiesOfConnections(johnIdentity, baseActivity));
    
    //mary connects with root
    Relationship maryRootRelationship = relationshipManager.inviteToConnect(maryIdentity, rootIdentity);
    relationshipManager.confirm(maryIdentity, rootIdentity);
    relationships.add(maryRootRelationship);
    
    assertEquals(5, activityStorageImpl.getNewerOnActivitiesOfConnections(maryIdentity, baseActivity, 10).size());
    assertEquals(5, activityStorageImpl.getNumberOfNewerOnActivitiesOfConnections(maryIdentity, baseActivity));
    
    for (Relationship rel : relationships) {
      relationshipManager.delete(rel);
    }
  }
  @MaxQueryNumber(1135)
  public void testGetOlderOnActivitiesOfConnections() throws Exception {
    List<Relationship> relationships = new ArrayList<Relationship> ();
    createActivities(3, maryIdentity);
    createActivities(1, demoIdentity);
    createActivities(2, johnIdentity);
    createActivities(2, rootIdentity);
    
    List<ExoSocialActivity> maryActivities = activityStorageImpl.getActivitiesOfIdentity(maryIdentity, 0, 10);
    assertEquals(3, maryActivities.size());
    
    //base activity is the first activity created by mary
    ExoSocialActivity baseActivity = maryActivities.get(2);
    
    //As mary has no connections, there are any activity on her connection stream
    assertEquals(0, activityStorageImpl.getOlderOnActivitiesOfConnections(maryIdentity, baseActivity, 10).size());
    assertEquals(0, activityStorageImpl.getNumberOfOlderOnActivitiesOfConnections(maryIdentity, baseActivity));
    
    //demo connect with mary
    Relationship maryDemoRelationship = relationshipManager.inviteToConnect(maryIdentity, demoIdentity);
    relationshipManager.confirm(maryIdentity, demoIdentity);
    relationships.add(maryDemoRelationship);
    
    baseActivity = activityStorageImpl.getActivitiesOfIdentity(demoIdentity, 0, 10).get(0);
    assertEquals(0, activityStorageImpl.getOlderOnActivitiesOfConnections(maryIdentity, baseActivity, 10).size());
    assertEquals(0, activityStorageImpl.getNumberOfOlderOnActivitiesOfConnections(maryIdentity, baseActivity));
    
    assertEquals(3, activityStorageImpl.getOlderOnActivitiesOfConnections(demoIdentity, baseActivity, 10).size());
    assertEquals(3, activityStorageImpl.getNumberOfOlderOnActivitiesOfConnections(demoIdentity, baseActivity));
    
    //john connects with mary
    Relationship maryJohnRelationship = relationshipManager.inviteToConnect(maryIdentity, johnIdentity);
    relationshipManager.confirm(maryIdentity, johnIdentity);
    relationships.add(maryJohnRelationship);
    
    baseActivity = activityStorageImpl.getActivitiesOfIdentity(johnIdentity, 0, 10).get(0);
    assertEquals(2, activityStorageImpl.getOlderOnActivitiesOfConnections(maryIdentity, baseActivity, 10).size());
    assertEquals(2, activityStorageImpl.getNumberOfOlderOnActivitiesOfConnections(maryIdentity, baseActivity));
    
    assertEquals(3, activityStorageImpl.getOlderOnActivitiesOfConnections(johnIdentity, baseActivity, 10).size());
    assertEquals(3, activityStorageImpl.getNumberOfOlderOnActivitiesOfConnections(johnIdentity, baseActivity));
    
    //mary connects with root
    Relationship maryRootRelationship = relationshipManager.inviteToConnect(maryIdentity, rootIdentity);
    relationshipManager.confirm(maryIdentity, rootIdentity);
    relationships.add(maryRootRelationship);
    
    baseActivity = activityStorageImpl.getActivitiesOfIdentity(rootIdentity, 0, 10).get(0);
    assertEquals(4, activityStorageImpl.getOlderOnActivitiesOfConnections(maryIdentity, baseActivity, 10).size());
    assertEquals(4, activityStorageImpl.getNumberOfOlderOnActivitiesOfConnections(maryIdentity, baseActivity));
    
    for (Relationship rel : relationships) {
      relationshipManager.delete(rel);
    }
  }
  @MaxQueryNumber(813)
  public void testGetNewerOnUserSpacesActivities() throws Exception {
    Space space = this.getSpaceInstance(spaceService, 0);
    tearDownSpaceList.add(space);
    Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
    
    int totalNumber = 10;
    ExoSocialActivity baseActivity = null;
    //demo posts activities to space
    for (int i = 0; i < totalNumber; i ++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle("activity title " + i);
      activity.setUserId(demoIdentity.getId());
      activityStorageImpl.saveActivity(spaceIdentity, activity);
      tearDownActivityList.add(activity);
      if (i == 0) {
        baseActivity = activity;
      }
    }
    
    assertEquals(9, activityStorageImpl.getNewerOnUserSpacesActivities(demoIdentity, baseActivity, 10).size());
    assertEquals(9, activityStorageImpl.getNumberOfNewerOnUserSpacesActivities(demoIdentity, baseActivity));
    //
    assertEquals(9, activityStorageImpl.getNewerOnSpaceActivities(spaceIdentity, baseActivity, 10).size());
    assertEquals(9, activityStorageImpl.getNumberOfNewerOnSpaceActivities(spaceIdentity, baseActivity));
    
    Space space2 = this.getSpaceInstance(spaceService, 1);
    tearDownSpaceList.add(space2);
    Identity spaceIdentity2 = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space2.getPrettyName(), false);
    //demo posts activities to space2
    for (int i = 0; i < totalNumber; i ++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle("activity title " + i);
      activity.setUserId(demoIdentity.getId());
      activityStorageImpl.saveActivity(spaceIdentity2, activity);
      tearDownActivityList.add(activity);
    }
    
    assertEquals(19, activityStorageImpl.getNewerOnUserSpacesActivities(demoIdentity, baseActivity, 20).size());
    assertEquals(19, activityStorageImpl.getNumberOfNewerOnUserSpacesActivities(demoIdentity, baseActivity));
  }
  @MaxQueryNumber(801)
  public void testGetOlderOnUserSpacesActivities() throws Exception {
    Space space = this.getSpaceInstance(spaceService, 0);
    tearDownSpaceList.add(space);
    Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
    
    int totalNumber = 5;
    ExoSocialActivity baseActivity = null;
    //demo posts activities to space
    for (int i = 0; i < totalNumber; i ++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle("activity title " + i);
      activity.setUserId(demoIdentity.getId());
      activityStorageImpl.saveActivity(spaceIdentity, activity);
      tearDownActivityList.add(activity);
      if (i == 4) {
        baseActivity = activity;
      }
    }
    
    assertEquals(4, activityStorageImpl.getOlderOnUserSpacesActivities(demoIdentity, baseActivity, 10).size());
    assertEquals(4, activityStorageImpl.getNumberOfOlderOnUserSpacesActivities(demoIdentity, baseActivity));
    //
    assertEquals(4, activityStorageImpl.getOlderOnSpaceActivities(spaceIdentity, baseActivity, 10).size());
    assertEquals(4, activityStorageImpl.getNumberOfOlderOnSpaceActivities(spaceIdentity, baseActivity));
    
    Space space2 = this.getSpaceInstance(spaceService, 1);
    tearDownSpaceList.add(space2);
    Identity spaceIdentity2 = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space2.getPrettyName(), false);
    //demo posts activities to space2
    for (int i = 0; i < totalNumber; i ++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle("activity title " + i);
      activity.setUserId(demoIdentity.getId());
      activityStorageImpl.saveActivity(spaceIdentity2, activity);
      tearDownActivityList.add(activity);
    }
    
    assertEquals(4, activityStorageImpl.getOlderOnUserSpacesActivities(demoIdentity, baseActivity, 10).size());
    assertEquals(4, activityStorageImpl.getNumberOfOlderOnUserSpacesActivities(demoIdentity, baseActivity));
  }
  @MaxQueryNumber(14)
  public void testGetNewerComments() {
    int totalNumber = 10;
    
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle("activity title");
    activity.setUserId(rootIdentity.getId());
    activityStorageImpl.saveActivity(rootIdentity, activity);
    tearDownActivityList.add(activity);
    
    for (int i = 0; i < totalNumber; i ++) {
      //John comments on Root's activity
      ExoSocialActivity comment = new ExoSocialActivityImpl();
      comment.setTitle("john comment " + i);
      comment.setUserId(johnIdentity.getId());
      activityStorageImpl.saveComment(activity, comment);
    }
    
    for (int i = 0; i < totalNumber; i ++) {
      //John comments on Root's activity
      ExoSocialActivity comment = new ExoSocialActivityImpl();
      comment.setTitle("demo comment " + i);
      comment.setUserId(demoIdentity.getId());
      activityStorageImpl.saveComment(activity, comment);
    }
    
    List<ExoSocialActivity> comments = activityStorageImpl.getComments(activity, 0, 20);
    assertEquals(20, comments.size());
    
    ExoSocialActivity baseComment = comments.get(0);
    
    assertEquals(19, activityStorageImpl.getNewerComments(activity, baseComment, 20).size());
    assertEquals(19, activityStorageImpl.getNumberOfNewerComments(activity, baseComment));
    
    baseComment = comments.get(9);
    assertEquals(10, activityStorageImpl.getNewerComments(activity, baseComment, 20).size());
    assertEquals(10, activityStorageImpl.getNumberOfNewerComments(activity, baseComment));
    
    baseComment = comments.get(19);
    assertEquals(0, activityStorageImpl.getNewerComments(activity, baseComment, 20).size());
    assertEquals(0, activityStorageImpl.getNumberOfNewerComments(activity, baseComment));
  }
  @MaxQueryNumber(530)
  public void testGetOlderComments() {
    int totalNumber = 10;
    
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle("activity title");
    activity.setUserId(rootIdentity.getId());
    activityStorageImpl.saveActivity(rootIdentity, activity);
    tearDownActivityList.add(activity);
    
    for (int i = 0; i < totalNumber; i ++) {
      //John comments on Root's activity
      ExoSocialActivity comment = new ExoSocialActivityImpl();
      comment.setTitle("john comment " + i);
      comment.setUserId(johnIdentity.getId());
      activityStorageImpl.saveComment(activity, comment);
    }
    
    for (int i = 0; i < totalNumber; i ++) {
      //John comments on Root's activity
      ExoSocialActivity comment = new ExoSocialActivityImpl();
      comment.setTitle("demo comment " + i);
      comment.setUserId(demoIdentity.getId());
      activityStorageImpl.saveComment(activity, comment);
    }
    
    List<ExoSocialActivity> comments = activityStorageImpl.getComments(activity, 0, 20);
    assertEquals(20, comments.size());
    
    ExoSocialActivity baseComment = comments.get(19);
    
    assertEquals(19, activityStorageImpl.getOlderComments(activity, baseComment, 20).size());
    assertEquals(19, activityStorageImpl.getNumberOfOlderComments(activity, baseComment));
    
    baseComment = comments.get(10);
    assertEquals(10, activityStorageImpl.getOlderComments(activity, baseComment, 20).size());
    assertEquals(10, activityStorageImpl.getNumberOfOlderComments(activity, baseComment));
    
    baseComment = comments.get(0);
    assertEquals(0, activityStorageImpl.getOlderComments(activity, baseComment, 20).size());
    assertEquals(0, activityStorageImpl.getNumberOfOlderComments(activity, baseComment));
  }
  @MaxQueryNumber(1263)
  public void testMentionersAndCommenters() throws Exception {
    ExoSocialActivity activity = createActivity(1);
    activity.setTitle("hello @demo @john");
    activityStorageImpl.saveActivity(rootIdentity, activity);
    tearDownActivityList.add(activity);
    
    
    ExoSocialActivity got = activityStorageImpl.getActivity(activity.getId());
    assertNotNull(got);
    assertEquals(2, got.getMentionedIds().length);
    
    ExoSocialActivity comment1 = new ExoSocialActivityImpl();
    comment1.setTitle("comment 1");
    comment1.setUserId(demoIdentity.getId());
    activityStorageImpl.saveComment(activity, comment1);
    ExoSocialActivity comment2 = new ExoSocialActivityImpl();
    comment2.setTitle("comment 2");
    comment2.setUserId(johnIdentity.getId());
    activityStorageImpl.saveComment(activity, comment2);
    
    got = activityStorageImpl.getActivity(activity.getId());
    assertEquals(2, got.getReplyToId().length);
    assertEquals(2, got.getCommentedIds().length);
    
    ExoSocialActivity comment3 = new ExoSocialActivityImpl();
    comment3.setTitle("hello @mary");
    comment3.setUserId(johnIdentity.getId());
    activityStorageImpl.saveComment(activity, comment3);
    
    got = activityStorageImpl.getActivity(activity.getId());
    assertEquals(3, got.getReplyToId().length);
    assertEquals(2, got.getCommentedIds().length);
    assertEquals(3, got.getMentionedIds().length);
    
    ExoSocialActivity gotComment = activityStorageImpl.getActivity(comment3.getId());
    assertEquals(1, gotComment.getMentionedIds().length);
    
    activityStorageImpl.deleteComment(activity.getId(), comment3.getId());
    
    got = activityStorageImpl.getActivity(activity.getId());
    assertEquals(2, got.getReplyToId().length);
    assertEquals(2, got.getCommentedIds().length);
    assertEquals(2, got.getMentionedIds().length);
  }
  
  private Space getSpaceInstance(SpaceService spaceService, int number) throws Exception {
    Space space = new Space();
    space.setDisplayName("my space " + number);
    space.setPrettyName(space.getDisplayName());
    space.setRegistration(Space.OPEN);
    space.setDescription("add new space " + number);
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setVisibility(Space.PUBLIC);
    space.setRegistration(Space.VALIDATION);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    space.setGroupId("/space/space" + number);
    space.setUrl(space.getPrettyName());
    String[] managers = new String[] {"demo"};
    String[] members = new String[] {"demo"};
    space.setManagers(managers);
    space.setMembers(members);
    spaceService.saveSpace(space, true);
    return space;
  }
  
  private void createActivities(int number, Identity owner) {
    for (int i = 0; i < number; i++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle("activity title " + i);
      activityStorageImpl.saveActivity(owner, activity);
      tearDownActivityList.add(activity);
    }
  }
  
  private ExoSocialActivity createActivity(int num) {
    //
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle("Activity "+ num);
    activity.setTitleId("TitleID: "+ activity.getTitle());
    activity.setType("UserActivity");
    activity.setBody("Body of "+ activity.getTitle());
    activity.setBodyId("BodyId of "+ activity.getTitle());
    activity.setLikeIdentityIds(new String[]{"demo", "mary"});
    activity.setMentionedIds(new String[]{"demo", "john"});
    activity.setCommentedIds(new String[]{});
    activity.setReplyToId(new String[]{});
    activity.setAppId("AppID");
    activity.setExternalId("External ID");
    
    return activity;
  }
  

}