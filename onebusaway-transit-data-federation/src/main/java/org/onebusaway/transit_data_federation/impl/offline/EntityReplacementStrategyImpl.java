package org.onebusaway.transit_data_federation.impl.offline;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.onebusaway.transit_data_federation.services.offline.EntityReplacementStrategy;

/**
 * {@link EntityReplacementStrategy} implementation that is used to map entity
 * ids for a particular type.
 * 
 * @author bdferris
 * @see EntityReplacementStrategy
 * @see EntityReplacementStrategyFactory
 */
public class EntityReplacementStrategyImpl implements EntityReplacementStrategy {

  private Map<Class<?>, Map<Serializable, Serializable>> _entityReplacement = new HashMap<Class<?>, Map<Serializable, Serializable>>();

  public void addEntityReplacement(Class<?> entityType, Serializable entityId,
      Serializable replacementEntityId) {
    Map<Serializable, Serializable> idMappings = _entityReplacement.get(entityType);
    if (idMappings == null) {
      idMappings = new HashMap<Serializable, Serializable>();
      _entityReplacement.put(entityType, idMappings);
    }
    idMappings.put(entityId, replacementEntityId);
  }

  @Override
  public boolean hasReplacementEntities(Class<?> entityType) {
    return _entityReplacement.containsKey(entityType);
  }

  @Override
  public boolean hasReplacementEntity(Class<?> entityType, Serializable entityId) {
    Map<Serializable, Serializable> idMappings = _entityReplacement.get(entityType);
    if (idMappings == null)
      return false;
    return idMappings.containsKey(entityId);
  }

  @Override
  public Serializable getReplacementEntityId(Class<?> entityType,
      Serializable entityId) {
    Map<Serializable, Serializable> idMappings = _entityReplacement.get(entityType);
    if (idMappings == null)
      return null;
    return idMappings.get(entityId);
  }

}
