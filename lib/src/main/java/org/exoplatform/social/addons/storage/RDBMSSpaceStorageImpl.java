package org.exoplatform.social.addons.storage;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.search.ESSpaceFilter;
import org.exoplatform.social.addons.search.SpaceSearchConnector;
import org.exoplatform.social.addons.storage.dao.SpaceDAO;
import org.exoplatform.social.addons.storage.dao.SpaceMemberDAO;
import org.exoplatform.social.addons.storage.entity.SpaceEntity;
import org.exoplatform.social.addons.storage.entity.SpaceMember.Status;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.entity.ProviderEntity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.space.SpaceFilter;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.SpaceStorageException;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.storage.exception.NodeNotFoundException;
import org.exoplatform.social.core.storage.impl.AbstractStorage;

public class RDBMSSpaceStorageImpl extends AbstractStorage implements SpaceStorage {

  /** Logger */
  private static final Log     LOG = ExoLogger.getLogger(RDBMSSpaceStorageImpl.class);

  private SpaceDAO             spaceDAO;

  private SpaceMemberDAO       spaceMemberDAO;

  private IdentityStorage      identityStorage;

  private SpaceSearchConnector spaceSearchConnector;

  public RDBMSSpaceStorageImpl(SpaceDAO spaceDAO,
                               SpaceMemberDAO spaceMemberDAO,
                               IdentityStorage identityStorage,
                               SpaceSearchConnector spaceSearchConnector) {
    this.spaceDAO = spaceDAO;
    this.identityStorage = identityStorage;
    this.spaceSearchConnector = spaceSearchConnector;
    this.spaceMemberDAO = spaceMemberDAO;
  }

  @Override
  @ExoTransactional
  public void deleteSpace(String id) throws SpaceStorageException {
    SpaceEntity entity = spaceDAO.find(Long.parseLong(id));
    if (entity != null) {
//      spaceMemberDAO.deleteBySpace(entity);
      spaceDAO.delete(entity);

      LOG.debug("Space {} removed", entity.getPrettyName());
    }
  }

  @Override
  public List<Space> getAccessibleSpaces(String userId) throws SpaceStorageException {
    return getAccessibleSpaces(userId, 0, -1);
  }

  @Override
  public List<Space> getAccessibleSpaces(String userId, long offset, long limit) throws SpaceStorageException {
    return getAccessibleSpacesByFilter(userId, null, offset, limit);
  }

  @Override
  public List<Space> getAccessibleSpacesByFilter(String userId, SpaceFilter spaceFilter, long offset, long limit) {
    return getSpaces(userId, Arrays.asList(Status.MANAGER, Status.MEMBER), spaceFilter, offset, limit);
  }

  @Override
  public int getAccessibleSpacesByFilterCount(String userId, SpaceFilter spaceFilter) {
    return getSpacesCount(userId, Arrays.asList(Status.MANAGER, Status.MEMBER), spaceFilter);
  }

  @Override
  public int getAccessibleSpacesCount(String userId) throws SpaceStorageException {
    return getAccessibleSpacesByFilterCount(userId, null);
  }

  @Override
  public List<Space> getAllSpaces() throws SpaceStorageException {
    List<Space> spaces = new LinkedList<>();

    for (SpaceEntity entity : spaceDAO.findAll()) {
      Space space = new Space();
      fillSpaceFromEntity(entity, space);
      spaces.add(space);
    }
    Collections.sort(spaces, new Comparator<Space>() {
      @Override
      public int compare(Space o1, Space o2) {
        return o1.getPrettyName().compareTo(o2.getPrettyName());
      }
    });
    return spaces;
  }

  @Override
  public int getAllSpacesByFilterCount(SpaceFilter spaceFilter) {
    return getSpacesCount(null, null, spaceFilter);
  }

  @Override
  public int getAllSpacesCount() throws SpaceStorageException {
    return getAllSpacesByFilterCount(null);
  }

  @Override
  public List<Space> getEditableSpaces(String userId) throws SpaceStorageException {
    return getEditableSpaces(userId, 0, -1);
  }

  @Override
  public List<Space> getEditableSpaces(String userId, long offset, long limit) throws SpaceStorageException {
    return getEditableSpacesByFilter(userId, null, offset, limit);
  }

  @Override
  public List<Space> getEditableSpacesByFilter(String userId, SpaceFilter spaceFilter, long offset, long limit) {
    return getSpaces(userId, Arrays.asList(Status.MANAGER), spaceFilter, offset, limit);
  }

  @Override
  public int getEditableSpacesByFilterCount(String userId, SpaceFilter spaceFilter) {
    return getSpacesCount(userId, Arrays.asList(Status.MANAGER), spaceFilter);
  }

  @Override
  public int getEditableSpacesCount(String userId) throws SpaceStorageException {
    return getEditableSpacesByFilterCount(userId, null);
  }

  @Override
  public List<Space> getInvitedSpaces(String userId) throws SpaceStorageException {
    return getInvitedSpaces(userId, 0, -1);
  }

  @Override
  public List<Space> getInvitedSpaces(String userId, long offset, long limit) throws SpaceStorageException {
    return getInvitedSpacesByFilter(userId, null, offset, limit);
  }

  @Override
  public List<Space> getInvitedSpacesByFilter(String userId, SpaceFilter spaceFilter, long offset, long limit) {
    return getSpaces(userId, Arrays.asList(Status.INVITED), spaceFilter, offset, limit);
  }

  @Override
  public int getInvitedSpacesByFilterCount(String userId, SpaceFilter spaceFilter) {
    return getSpacesCount(userId, Arrays.asList(Status.INVITED), spaceFilter);
  }

  @Override
  public int getInvitedSpacesCount(String userId) throws SpaceStorageException {
    return getInvitedSpacesByFilterCount(userId, null);
  }

  @Override
  public List<Space> getLastAccessedSpace(SpaceFilter spaceFilter, int offset, int limit) throws SpaceStorageException {
    // TODO delegate this method to IdentityDAO
    return getMemberSpacesByFilter(spaceFilter.getRemoteId(), spaceFilter, offset, limit);
  }

  @Override
  public int getLastAccessedSpaceCount(SpaceFilter spaceFilter) throws SpaceStorageException {
    // TODO delegate this method to IdentityDAO
    return getMemberSpacesCount(spaceFilter.getRemoteId());
  }

  @Override
  public List<Space> getLastSpaces(int limit) {
    List<SpaceEntity> entities = spaceDAO.getLastSpaces(limit);
    return buildList(entities);
  }

  @Override
  public List<String> getMemberSpaceIds(String identityId, int offset, int limit) throws SpaceStorageException {
    Identity identity = identityStorage.findIdentityById(identityId);
    List<Space> spaces = getMemberSpaces(identity.getRemoteId(), offset, limit);

    List<String> ids = new LinkedList<>();
    for (Space space : spaces) {
      Identity spaceIdentity = identityStorage.findIdentity(SpaceIdentityProvider.NAME, space.getPrettyName());
      if (spaceIdentity != null) {
        ids.add(spaceIdentity.getId());
      }
    }
    return ids;
  }

  @Override
  public List<Space> getMemberSpaces(String userId) throws SpaceStorageException {
    return getMemberSpaces(userId, 0, -1);
  }

  @Override
  public List<Space> getMemberSpaces(String userId, long offset, long limit) throws SpaceStorageException {
    return getMemberSpacesByFilter(userId, null, offset, limit);
  }

  @Override
  public List<Space> getMemberSpacesByFilter(String userId, SpaceFilter spaceFilter, long offset, long limit) {
    return getSpaces(userId, Arrays.asList(Status.MEMBER), spaceFilter, offset, limit);
  }

  @Override
  public int getMemberSpacesByFilterCount(String userId, SpaceFilter spaceFilter) {
    return getSpacesCount(userId, Arrays.asList(Status.MEMBER), spaceFilter);
  }

  @Override
  public int getMemberSpacesCount(String userId) throws SpaceStorageException {
    return getMemberSpacesByFilterCount(userId, null);
  }

  @Override
  public int getNumberOfMemberPublicSpaces(String userId) {
    ESSpaceFilter filter = new ESSpaceFilter();
    filter.setNotHidden(true);
    return getSpacesCount(userId, Arrays.asList(Status.MEMBER), filter);
  }

  @Override
  public List<Space> getPendingSpaces(String userId) throws SpaceStorageException {
    return getPendingSpaces(userId, 0, -1);
  }

  @Override
  public List<Space> getPendingSpaces(String userId, long offset, long limit) throws SpaceStorageException {
    return getPendingSpacesByFilter(userId, null, offset, limit);
  }

  @Override
  public List<Space> getPendingSpacesByFilter(String userId, SpaceFilter spaceFilter, long offset, long limit) {
    return getSpaces(userId, Arrays.asList(Status.PENDING), spaceFilter, offset, limit);
  }

  @Override
  public int getPendingSpacesByFilterCount(String userId, SpaceFilter spaceFilter) {
    return getSpacesCount(userId, Arrays.asList(Status.PENDING), spaceFilter);
  }

  @Override
  public int getPendingSpacesCount(String userId) throws SpaceStorageException {
    return getPendingSpacesByFilterCount(userId, null);
  }

  @Override
  public List<Space> getPublicSpaces(String userId) throws SpaceStorageException {
    return getPublicSpaces(userId, 0, -1);
  }

  @Override
  public List<Space> getPublicSpaces(String userId, long offset, long limit) throws SpaceStorageException {
    return getPublicSpacesByFilter(userId, null, offset, limit);
  }

  @Override
  public List<Space> getPublicSpacesByFilter(String userId, SpaceFilter spaceFilter, long offset, long limit) {
    return getSpaces(userId, Arrays.asList(Status.values()), spaceFilter, offset, limit);
  }

  @Override
  public int getPublicSpacesByFilterCount(String userId, SpaceFilter spaceFilter) {
    return getSpacesCount(userId, Arrays.asList(Status.values()), spaceFilter);
  }

  @Override
  public int getPublicSpacesCount(String userId) throws SpaceStorageException {
    return getPublicSpacesByFilterCount(userId, null);
  }

  @Override
  public Space getSpaceByDisplayName(String spaceDisplayName) throws SpaceStorageException {
    ESSpaceFilter filter = new ESSpaceFilter();
    filter.setDisplayName(spaceDisplayName);

    List<Space> spaces = getSpacesByFilter(filter, 0, 1);
    if (spaces.isEmpty()) {
      return null;
    } else {
      return spaces.iterator().next();
    }
  }

  @Override
  public Space getSpaceByGroupId(String groupId) throws SpaceStorageException {
    SpaceEntity entity = spaceDAO.getSpaceByGroupId(groupId);
    if (entity != null) {
      Space space = new Space();
      fillSpaceFromEntity(entity, space);
      return space;
    } else {
      return null;
    }
  }

  @Override
  public Space getSpaceById(String id) throws SpaceStorageException {
    Long spaceId;
    try {
      spaceId = Long.parseLong(id);
    } catch (Exception ex) {
      return null;
    }
    SpaceEntity entity = spaceDAO.find(spaceId);
    if (entity != null) {
      Space space = new Space();
      fillSpaceFromEntity(entity, space);
      return space;
    } else {
      return null;
    }
  }

  @Override
  public Space getSpaceByPrettyName(String spacePrettyName) throws SpaceStorageException {
    ESSpaceFilter filter = new ESSpaceFilter();
    filter.setPrettyName(spacePrettyName);

    List<Space> spaces = getSpacesByFilter(filter, 0, 1);

    if (spaces.isEmpty()) {
      return null;
    } else {
      return spaces.iterator().next();
    }
  }

  @Override
  public Space getSpaceByUrl(String url) throws SpaceStorageException {
    SpaceEntity entity = spaceDAO.getSpaceByURL(url);
    if (entity != null) {
      Space space = new Space();
      fillSpaceFromEntity(entity, space);
      return space;
    } else {
      return null;
    }
  }

  @Override
  public Space getSpaceSimpleById(String id) throws SpaceStorageException {
    Long spaceId;
    try {
      spaceId = Long.parseLong(id);
    } catch (Exception ex) {
      return null;
    }
    SpaceEntity entity = spaceDAO.find(spaceId);
    if (entity != null) {
      Space space = new Space();
      fillSpaceSimpleFromEntity(entity, space);
      return space;
    } else {
      return null;
    }
  }

  @Override
  public List<Space> getSpaces(long offset, long limit) throws SpaceStorageException {
    return getSpacesByFilter(null, offset, limit);
  }

  @Override
  public List<Space> getSpacesByFilter(SpaceFilter spaceFilter, long offset, long limit) {
    return getSpaces(null, null, spaceFilter, offset, limit);
  }

  @Override
  public List<Space> getUnifiedSearchSpaces(String userId,
                                            SpaceFilter spaceFilter,
                                            long offset,
                                            long limit) throws SpaceStorageException {
    ESSpaceFilter esFilter = new ESSpaceFilter();
    esFilter.setSpaceFilter(spaceFilter).setUnifiedSearch(true);
    return getSpacesByFilter(spaceFilter, offset, limit);
  }

  @Override
  public int getUnifiedSearchSpacesCount(String userId, SpaceFilter spaceFilter) throws SpaceStorageException {
    ESSpaceFilter esFilter = new ESSpaceFilter();
    esFilter.setSpaceFilter(spaceFilter).setUnifiedSearch(true);
    return getSpacesCount(null, null, spaceFilter);
  }

  @Override
  public List<Space> getVisibleSpaces(String userId, SpaceFilter spaceFilter) throws SpaceStorageException {
    return getVisibleSpaces(userId, spaceFilter, 0, -1);
  }

  @Override
  public List<Space> getVisibleSpaces(String userId,
                                      SpaceFilter spaceFilter,
                                      long offset,
                                      long limit) throws SpaceStorageException {
    ESSpaceFilter esFilter = new ESSpaceFilter();
    esFilter.setSpaceFilter(spaceFilter);
    esFilter.addStatus(userId, Status.MEMBER).addStatus(userId, Status.MANAGER).addStatus(userId, Status.INVITED);
    esFilter.setIncludePrivate(true);
    return getSpacesByFilter(spaceFilter, offset, limit);
  }

  @Override
  public int getVisibleSpacesCount(String userId, SpaceFilter spaceFilter) throws SpaceStorageException {
    ESSpaceFilter esFilter = new ESSpaceFilter();
    esFilter.setSpaceFilter(spaceFilter);
    esFilter.addStatus(userId, Status.MEMBER).addStatus(userId, Status.MANAGER).addStatus(userId, Status.INVITED);
    esFilter.setIncludePrivate(true);
    return getSpacesCount(userId, null, spaceFilter);
  }

  @Override
  public List<Space> getVisitedSpaces(SpaceFilter spaceFilter, int offset, int limit) throws SpaceStorageException {
    // TODO delegate this to IdentityDAO
    return Collections.emptyList();
  }

  @Override
  public void renameSpace(Space space, String newDisplayName) throws SpaceStorageException {
    renameSpace(null, space, newDisplayName);
  }

  @Override
  public void renameSpace(String remoteId, Space space, String newDisplayName) throws SpaceStorageException {
    SpaceEntity entity;

    try {
      String oldPrettyName = space.getPrettyName();

      space.setDisplayName(newDisplayName);
      space.setPrettyName(space.getDisplayName());
      space.setUrl(SpaceUtils.cleanString(newDisplayName));

      entity = spaceDAO.find(Long.parseLong(space.getId()));
      entity.buildFrom(space);

      // change profile of space
      Identity identitySpace = identityStorage.findIdentity(SpaceIdentityProvider.NAME, oldPrettyName);

      if (identitySpace != null) {
        Profile profileSpace = identitySpace.getProfile();
        profileSpace.setProperty(Profile.FIRST_NAME, space.getDisplayName());
        profileSpace.setProperty(Profile.USERNAME, space.getPrettyName());
        // profileSpace.setProperty(Profile.AVATAR_URL, space.getAvatarUrl());
        profileSpace.setProperty(Profile.URL, space.getUrl());

        identityStorage.saveProfile(profileSpace);

        identitySpace.setRemoteId(space.getPrettyName());
        // TODO remove this after finish RDBMSIdentityStorage
        renameIdentity(identitySpace);
      }

      //
      LOG.debug(String.format("Space %s (%s) saved", space.getPrettyName(), space.getId()));

    } catch (NodeNotFoundException e) {
      throw new SpaceStorageException(SpaceStorageException.Type.FAILED_TO_RENAME_SPACE, e.getMessage(), e);
    }
  }

  @Override
  public void saveSpace(Space space, boolean isNew) throws SpaceStorageException {
    if (isNew) {
      SpaceEntity entity = new SpaceEntity();
      entity = entity.buildFrom(space);

      //
      spaceDAO.create(entity);
      fillSpaceFromEntity(entity, space);
    } else {
      Long id = Long.parseLong(space.getId());
      SpaceEntity entity = spaceDAO.find(id);

      if (entity != null) {
        entity = entity.buildFrom(space);
        //
        spaceDAO.update(entity);
      } else {
        throw new SpaceStorageException(SpaceStorageException.Type.FAILED_TO_SAVE_SPACE);
      }
    }

    LOG.debug("Space {} ({}) saved", space.getPrettyName(), space.getId());
  }

  @Override
  public void updateSpaceAccessed(String arg0, Space arg1) throws SpaceStorageException {
    // TODO delegate this method to IdentityDAO
  }

  /**
   * Fills {@link Space}'s properties to {@link SpaceEntity}'s.
   *
   * @param entity the space entity
   * @param space the space pojo for services
   */
  private void fillSpaceFromEntity(SpaceEntity entity, Space space) {
    fillSpaceSimpleFromEntity(entity, space);

    space.setPendingUsers(entity.getPendingMembersId());
    space.setInvitedUsers(entity.getInvitedMembersId());

    //
    String[] members = entity.getMembersId();
    String[] managers = entity.getManagerMembersId();

    //
    Set<String> membersList = new HashSet<String>();
    if (members != null)
      membersList.addAll(Arrays.asList(members));
    if (managers != null)
      membersList.addAll(Arrays.asList(managers));

    //
    space.setMembers(membersList.toArray(new String[] {}));
    space.setManagers(entity.getManagerMembersId());
  }

  /**
   * Add this method to resolve SOC-3439
   * 
   * @param identity
   * @throws NodeNotFoundException
   */
  private void renameIdentity(Identity identity) throws NodeNotFoundException {
    ProviderEntity providerEntity = getProviderRoot().getProvider(identity.getProviderId());
    // Move identity
    IdentityEntity identityEntity = _findById(IdentityEntity.class, identity.getId());
    providerEntity.getIdentities().put(identity.getRemoteId(), identityEntity);

    identityEntity.setRemoteId(identity.getRemoteId());
  }

  private List<Space> getSpaces(String userId, List<Status> status, SpaceFilter spaceFilter, long offset, long limit) {
    ESSpaceFilter filter = new ESSpaceFilter();
    filter.setSpaceFilter(spaceFilter);
    if (status != null && userId != null) {
      for (Status st : status) {
        filter.addStatus(userId, st);
      }
    }
    return spaceSearchConnector.search(filter, offset, limit);
  }

  private int getSpacesCount(String userId, List<Status> status, SpaceFilter spaceFilter) {
    ESSpaceFilter filter = new ESSpaceFilter();
    filter.setSpaceFilter(spaceFilter);
    if (userId != null && status != null) {
      for (Status st : status) {
        filter.addStatus(userId, st);
      }
    }
    return spaceSearchConnector.count(filter);
  }

  private List<Space> buildList(List<SpaceEntity> spaceEntities) {
    List<Space> spaces = new LinkedList<>();
    if (spaceEntities != null) {
      for (SpaceEntity entity : spaceEntities) {
        Space space = new Space();
        fillSpaceFromEntity(entity, space);
        spaces.add(space);
      }
    }
    return spaces;
  }

  /**
   * Fills {@link Space}'s properties to {@link SpaceEntity}'s.
   *
   * @param entity the space entity from chromattic
   * @param space the space pojo for services
   */
  private void fillSpaceSimpleFromEntity(SpaceEntity entity, Space space) {
    space.setApp(entity.getApp());
    space.setId(String.valueOf(entity.getId()));
    space.setDisplayName(entity.getDisplayName());
    space.setPrettyName(entity.getPrettyName());
    space.setRegistration(entity.getRegistration());
    space.setDescription(entity.getDescription());
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setVisibility(entity.getVisibility());
    space.setPriority(entity.getPriority());
    space.setGroupId(entity.getGroupId());
    space.setUrl(entity.getUrl());
    space.setCreatedTime(entity.getCreatedTime());

    if (entity.getAvatarLastUpdated() != null) {
      try {
        // TODO url for space avatar
        String url = "";
        space.setAvatarUrl(url);
      } catch (Exception e) {
        LOG.warn("Failed to build avatar url: " + e.getMessage());
      }
    }
    space.setAvatarLastUpdated(entity.getAvatarLastUpdated());
  }

  public void setSpaceSearchConnector(SpaceSearchConnector spaceSearchConnector) {
    this.spaceSearchConnector = spaceSearchConnector;
  }

}