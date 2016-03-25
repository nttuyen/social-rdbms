package org.exoplatform.social.addons.storage.entity;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@Embeddable
@ExoEntity
public class AppEntity implements Serializable {

  private static final long serialVersionUID = -8893364434133832686L;

  private static final Log  LOG              = ExoLogger.getLogger(AppEntity.class);

  @Column(name = "APP_ID", length = 50)
  private String            appId;

  @Column(name = "APP_NAME", length = 50)
  private String            appName;

  @Column(name = "REMOVABLE")
  private boolean           isRemovable;

  @Column(name = "STATUS")
  private Status            status;

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public boolean isRemovable() {
    return isRemovable;
  }

  public void setRemovable(boolean isRemovable) {
    this.isRemovable = isRemovable;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public static Set<AppEntity> parse(String apps) {
    if (apps == null) {
      return Collections.emptySet();
    }

    Set<AppEntity> entities = new HashSet<>();
    for (String app : apps.split(",")) {
      String[] appPart = app.split(":");
      
      // an application status is composed with the form of
      // [appId:appName:isRemovableString:status]
      if (appPart.length == 4) {
        AppEntity entity = new AppEntity();       
        entity.setAppId(appPart[0]);
        entity.setAppName(appPart[1]);
        entity.setRemovable(Boolean.parseBoolean(appPart[2]));
        entity.setStatus(Status.valueOf(appPart[3].toUpperCase()));
        
        entities.add(entity);
      } else {
        LOG.warn("Can't parse app string", app);
      }
    }
    return entities;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(getAppId());
    builder.append(":").append(getAppName());
    builder.append(":").append(isRemovable());
    builder.append(":").append(getStatus().name());
    return builder.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((appId == null) ? 0 : appId.hashCode());
    result = prime * result + ((appName == null) ? 0 : appName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    AppEntity other = (AppEntity) obj;
    if (appId == null) {
      if (other.appId != null)
        return false;
    } else if (!appId.equals(other.appId))
      return false;
    if (appName == null) {
      if (other.appName != null)
        return false;
    } else if (!appName.equals(other.appName))
      return false;
    return true;
  }
  
  public static enum Status {
    ACTIVE, DEACTIVE, INSTALLED
  }
}