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
package org.exoplatform.social.addons.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.search.ExtendProfileFilter;
import org.exoplatform.social.addons.search.ProfileSearchConnector;
import org.exoplatform.social.addons.storage.dao.ConnectionDAO;
import org.exoplatform.social.addons.storage.dao.IdentityDAO;
import org.exoplatform.social.addons.storage.entity.ConnectionEntity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.relationship.model.Relationship.Type;
import org.exoplatform.social.core.storage.RelationshipStorageException;
import org.exoplatform.social.core.storage.impl.RelationshipStorageImpl;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 3, 2015  
 */
public class RDBMSRelationshipStorageImpl extends RelationshipStorageImpl {

  private static final Log LOG = ExoLogger.getLogger(RDBMSRelationshipStorageImpl.class);
  
  private final ConnectionDAO connectionDAO;
  private final IdentityDAO identityDAO;
  private final RDBMSIdentityStorageImpl identityStorage;
  private final ProfileSearchConnector profileESConnector;

  public RDBMSRelationshipStorageImpl(RDBMSIdentityStorageImpl identityStorage, ConnectionDAO connectionDAO, IdentityDAO identityDAO, ProfileSearchConnector profileESConnector) {
    super(identityStorage);
    this.connectionDAO = connectionDAO;
    this.identityDAO = identityDAO;
    this.identityStorage = identityStorage;
    this.profileESConnector = profileESConnector;
  }

  @Override
  @ExoTransactional
  public Relationship saveRelationship(Relationship relationship) throws RelationshipStorageException {
    if (relationship.getId() == null) {//create new relationship

      ConnectionEntity entity = connectionDAO.getConnection(relationship.getSender(), relationship.getReceiver());
      if (entity == null) {
        entity = new ConnectionEntity();
      }

      entity.setReceiver(identityDAO.find(Long.valueOf(relationship.getReceiver().getId())));
      entity.setSender(identityDAO.find(Long.valueOf(relationship.getSender().getId())));
      entity.setStatus(relationship.getStatus());
      entity.setUpdatedDate(new Date());
      //
      connectionDAO.create(entity);
      relationship.setId(Long.toString(entity.getId()));

    } else {//update an relationship
      ConnectionEntity entity = connectionDAO.getConnection(relationship.getSender(), relationship.getReceiver());
      entity.setStatus(relationship.getStatus());
      entity.setUpdatedDate(new Date());
      connectionDAO.update(entity);
    }
    //
    return relationship;
  }
  
  @Override
  @ExoTransactional
  public void removeRelationship(Relationship relationship) throws RelationshipStorageException {
    ConnectionEntity connection = connectionDAO.getConnection(relationship.getSender(), relationship.getReceiver());
    if (connection != null) {
      connectionDAO.delete(connection);
    }
  }
  
  @Override
  @ExoTransactional
  public Relationship getRelationship(Identity identity1, Identity identity2) throws RelationshipStorageException {
    if (identity1==null) {
      throw new IllegalArgumentException("Argument identity1 is null");
    }
    if (identity2==null) {
      throw new IllegalArgumentException("Argument identity2 is null");
    }
    ConnectionEntity item = connectionDAO.getConnection(identity1, identity2);
    if (item == null) {
      item = connectionDAO.getConnection(identity2, identity1);
    }
    return convertRelationshipItemToRelationship(item);
  }
  
  @Override
  public Relationship getRelationship(String relationshipId) throws RelationshipStorageException {
    return convertRelationshipItemToRelationship(connectionDAO.find(Long.valueOf(relationshipId)));
  }
  
  @Override
  public List<Identity> getConnections(Identity identity) throws RelationshipStorageException {
    return getConnections(identity, 0, -1);
  }
  
  @Override
  public List<Identity> getConnections(Identity identity, long offset, long limit) throws RelationshipStorageException {
    return convertRelationshipEntitiesToIdentities(connectionDAO.getConnections(identity, Relationship.Type.CONFIRMED, offset, limit), identity.getId());
  }
  
  @Override
  public int getConnectionsCount(Identity identity) throws RelationshipStorageException {
    return connectionDAO.getConnectionsCount(identity, Relationship.Type.CONFIRMED);
  }
  
  @Override
  public int getRelationshipsCount(Identity identity) throws RelationshipStorageException {
    return connectionDAO.getConnectionsCount(identity, null);
  }
  
  @Override//TODO need review again
  public List<Relationship> getRelationships(Identity identity, Relationship.Type type, List<Identity> listCheckIdentity) throws RelationshipStorageException {
    return getRelationships(identity, type);
  }
  
  @Override
  public List<Relationship> getReceiverRelationships(Identity receiver, Relationship.Type type, List<Identity> listCheckIdentity) throws RelationshipStorageException {
    return getRelationships(null, receiver, type);
  }
  
  @Override
  public List<Relationship> getSenderRelationships(Identity sender, Relationship.Type type, List<Identity> listCheckIdentity) throws RelationshipStorageException {
    return getRelationships(sender, null, type);
  }
  
  public List<Relationship> getRelationships(Identity identity, Relationship.Type type) {
    return convertRelationshipEntitiesToRelationships(connectionDAO.getConnections(identity, type, 0, -1));
  }

  public List<Relationship> getRelationships(Identity sender, Identity receiver, Type type) {
    return convertRelationshipEntitiesToRelationships(connectionDAO.getConnections(sender, receiver, type));
  }
  
  @Override
  public List<Identity> getOutgoingRelationships(Identity sender, long offset, long limit) throws RelationshipStorageException {
    return convertRelationshipEntitiesToIdentities(connectionDAO.getConnections(sender, Relationship.Type.OUTGOING, offset, limit), sender.getId());
  }
  
  @Override
  public int getOutgoingRelationshipsCount(Identity sender) throws RelationshipStorageException {
    return connectionDAO.getConnectionsCount(sender, Relationship.Type.OUTGOING);
  }
  
  @Override
  public List<Identity> getIncomingRelationships(Identity receiver, long offset, long limit) throws RelationshipStorageException {
    return convertRelationshipEntitiesToIdentities(connectionDAO.getConnections(receiver, Relationship.Type.INCOMING, offset, limit), receiver.getId());
  }
  
  @Override
  public int getIncomingRelationshipsCount(Identity receiver) throws RelationshipStorageException {
    return connectionDAO.getConnectionsCount(receiver, Relationship.Type.INCOMING);
  }
  
  @Override
  public List<Identity> getLastConnections(Identity identity, int limit) throws RelationshipStorageException {
    return convertRelationshipEntitiesToIdentities(connectionDAO.getLastConnections(identity, limit), identity.getId());
  }
  
  private List<Identity> convertRelationshipEntitiesToIdentities(List<ConnectionEntity> connections, String ownerId) {
    List<Identity> identities = new ArrayList<Identity>();
    if (connections == null) return identities;
    for (ConnectionEntity item : connections) {
      identities.add(getIdentityFromRelationshipItem(item, ownerId));
    }
    return identities;
  }
  
  private List<Relationship> convertRelationshipEntitiesToRelationships(List<ConnectionEntity> connections) {
    List<Relationship> relationships = new ArrayList<Relationship>();
    if (connections == null) return relationships;
    for (ConnectionEntity item : connections) {
      relationships.add(convertRelationshipItemToRelationship(item));
    }
    return relationships;
  }

  private Identity getIdentityFromRelationshipItem(ConnectionEntity item, String ownerId) {
    Identity identity = null;
    if (ownerId.equals(item.getSender().getStringId())) {
      identity = identityStorage.findIdentityById(item.getReceiver().getStringId());
    } else {
      identity = identityStorage.findIdentityById(item.getSender().getStringId());
    }
    if (identity == null) return null;
    //load profile
    Profile profile = identityStorage.loadProfile(identity.getProfile());
    identity.setProfile(profile);
    return identity;
  }
  
  private Relationship convertRelationshipItemToRelationship(ConnectionEntity item) {
    if (item == null) return null;
    //
    Relationship relationship = new Relationship(Long.toString(item.getId()));
    relationship.setId(String.valueOf(item.getId()));
    relationship.setSender(identityStorage.findIdentityById(item.getSender().getStringId()));
    relationship.setReceiver(identityStorage.findIdentityById(item.getReceiver().getStringId()));
    relationship.setStatus(item.getStatus());
    return relationship;
  }

  @Override
  public List<Relationship> getSenderRelationships(String senderId, Type type, List<Identity> listCheckIdentity) throws RelationshipStorageException {
    Identity sender = identityStorage.findIdentityById(senderId);
    return getSenderRelationships(sender, type, listCheckIdentity);
  }

  @Override
  public boolean hasRelationship(Identity identity1, Identity identity2, String relationshipPath) {
    Relationship r = getRelationship(identity1, identity2);
    return r != null && Relationship.Type.CONFIRMED.equals(r.getStatus());
  }

  @Override
  public List<Identity> getRelationships(Identity identity, long offset, long limit) throws RelationshipStorageException {
    return convertRelationshipEntitiesToIdentities(connectionDAO.getConnections(identity, null, offset, limit), identity.getId());
  }

  @Override
  public List<Identity> getConnectionsByFilter(Identity existingIdentity, ProfileFilter profileFilter, long offset, long limit) throws RelationshipStorageException {
    //return profileESConnector.search(existingIdentity, profileFilter, Relationship.Type.CONFIRMED, offset, limit);
    return searchConnectionByFilter(existingIdentity, Type.CONFIRMED, profileFilter, offset, limit);
  }

  @Override
  public List<Identity> getIncomingByFilter(Identity existingIdentity, ProfileFilter profileFilter, long offset, long limit) throws RelationshipStorageException {
    return searchConnectionByFilter(existingIdentity, Type.INCOMING, profileFilter, offset, limit);
  }

  @Override
  public List<Identity> getOutgoingByFilter(Identity existingIdentity, ProfileFilter profileFilter, long offset, long limit) throws RelationshipStorageException {
    return searchConnectionByFilter(existingIdentity, Type.OUTGOING, profileFilter, offset, limit);
  }

  @Override
  public int getConnectionsCountByFilter(Identity existingIdentity, ProfileFilter profileFilter) throws RelationshipStorageException {
    return countConnectionByFilter(existingIdentity, Type.CONFIRMED, profileFilter);
  }

  @Override
  public int getIncomingCountByFilter(Identity existingIdentity, ProfileFilter profileFilter) throws RelationshipStorageException {
    return countConnectionByFilter(existingIdentity, Type.INCOMING, profileFilter);
  }

  @Override
  public int getOutgoingCountByFilter(Identity existingIdentity, ProfileFilter profileFilter) throws RelationshipStorageException {
    return countConnectionByFilter(existingIdentity, Type.OUTGOING, profileFilter);
  }

  @Override
  public Map<Identity, Integer> getSuggestions(Identity identity, int maxConnections, int maxConnectionsToLoad, int maxSuggestions) throws RelationshipStorageException {
    if (maxConnectionsToLoad > 0 && maxConnections > maxConnectionsToLoad)
      maxConnectionsToLoad = maxConnections;
    // Get identities level 1
   Set<Identity> relationIdLevel1 = new HashSet<Identity>();
   int size = getConnectionsCount(identity);
   // The ideal limit of connection to treat however we could need to go beyond this limit
   // if we cannot reach the expected amount of suggestions
   int endIndex;
   Random random = new Random();
   List<Identity> connections;
   if (size > maxConnectionsToLoad && maxConnectionsToLoad > 0 && maxConnections > 0) {
     // The total amount of connections is bigger than the maximum allowed
     // We will then load only a random sample to reduce the best we can the 
     // required time for this task 
     int startIndex = random.nextInt(size - maxConnectionsToLoad);
     endIndex = maxConnections;
     connections= getConnections(identity, startIndex, maxConnectionsToLoad);
   } else {
     // The total amount of connections is less than the maximum allowed
     // We call load everything
     endIndex = size;
     connections= getConnections(identity, 0, size);
   }
   // we need to load all the connections
   for (int i = 0; i < connections.size(); i++) {
     Identity id = connections.get(i);
     relationIdLevel1.add(id);
   }
   relationIdLevel1.remove(identity);

   // Get identities level 2 (suggested Identities)
   Map<Identity, Integer> suggestedIdentities = new HashMap<Identity, Integer>();
   Iterator<Identity> it = relationIdLevel1.iterator();
   for (int j = 0; j < size && it.hasNext(); j++) {
     Identity id = it.next();
     // We check if we reach the limit of connections to treat and if we have enough suggestions
     if (j >= endIndex && suggestedIdentities.size() > maxSuggestions && maxSuggestions > 0)
       break;
     int allConnSize = getConnectionsCount(id);
     int allConnStartIndex = 0;
     if (allConnSize > maxConnections && maxConnections > 0) {
       // The current identity has more connections that the allowed amount so we will treat a sample
       allConnStartIndex = random.nextInt(allConnSize - maxConnections);
       connections = getConnections(id, allConnStartIndex, maxConnections);
     } else {
       // The current identity doesn't have more connections that the allowed amount so we will 
       // treat all of them
       connections = getConnections(id, 0, allConnSize);
     }
     for (int i = 0; i < connections.size(); i++) {
       Identity ids = connections.get(i);
       // We check if the current connection is not already part of the connections of the identity
       // for which we seek some suggestions
       if (!relationIdLevel1.contains(ids) && !ids.equals(identity) && !ids.isDeleted()
            && getRelationship(ids, identity) == null) {
         Integer commonIdentities = suggestedIdentities.get(ids);
         if (commonIdentities == null) {
           commonIdentities = new Integer(1);
         } else {
           commonIdentities = new Integer(commonIdentities.intValue() + 1);
         }
         suggestedIdentities.put(ids, commonIdentities);
       }
     }
   }
   NavigableMap<Integer, List<Identity>> groupByCommonConnections = new TreeMap<Integer, List<Identity>>();
   // This for loop allows to group the suggestions by total amount of common connections
   for (Identity id : suggestedIdentities.keySet()) {
     Integer commonIdentities = suggestedIdentities.get(id);
     List<Identity> ids = groupByCommonConnections.get(commonIdentities);
     if (ids == null) {
       ids = new ArrayList<Identity>();
       groupByCommonConnections.put(commonIdentities, ids);
     }
     ids.add(id);
   }
   Map<Identity, Integer> suggestions = new LinkedHashMap<Identity, Integer>();
   int suggestionLeft = maxSuggestions;
   // We iterate over the suggestions starting from the suggestions with the highest amount of common
   // connections
   main: for (Integer key : groupByCommonConnections.descendingKeySet()) {
     List<Identity> ids = groupByCommonConnections.get(key);
     for (Identity id : ids) {
       suggestions.put(id, key);
       // We stop once we have enough suggestions
       if (maxSuggestions > 0 && --suggestionLeft == 0)
         break main;
     }
   }
   return suggestions;
  }

  private List<Identity> searchConnectionByFilter(Identity owner, Relationship.Type status, ProfileFilter profileFilter, long offset, long limit) {
    ExtendProfileFilter xFilter = new ExtendProfileFilter(profileFilter);
    xFilter.setConnection(owner);
    xFilter.setConnectionStatus(status);

    ListAccess<Identity> list = identityStorage.findByFilter(xFilter);
    try {
      return Arrays.asList(list.load((int)offset, (int)limit));
    } catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
      return Collections.emptyList();
    }
  }

  private int countConnectionByFilter(Identity owner, Relationship.Type status, ProfileFilter profileFilter) {
    ExtendProfileFilter xFilter = new ExtendProfileFilter(profileFilter);
    xFilter.setConnection(owner);
    xFilter.setConnectionStatus(status);

    ListAccess<Identity> list = identityStorage.findByFilter(xFilter);
    try {
      return list.getSize();
    } catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
      return 0;
    }
  }
}
