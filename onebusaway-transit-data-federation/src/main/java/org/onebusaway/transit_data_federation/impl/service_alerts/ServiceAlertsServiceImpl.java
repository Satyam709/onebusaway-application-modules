package org.onebusaway.transit_data_federation.impl.service_alerts;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.onebusaway.collections.ConcurrentCollectionsLibrary;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectedVehicleJourneyBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data.model.service_alerts.SituationBean;
import org.onebusaway.transit_data.model.service_alerts.SituationExchangeDeliveryBean;
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlertsService;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ServiceAlertsServiceImpl implements ServiceAlertsService {

  private static Logger _log = LoggerFactory.getLogger(ServiceAlertsServiceImpl.class);

  private static final String ID_SEPARATOR = "_|_";

  private ConcurrentMap<String, SituationBean> _situations = new ConcurrentHashMap<String, SituationBean>();

  private ConcurrentMap<String, Set<String>> _situationIdsByLineId = new ConcurrentHashMap<String, Set<String>>();

  private ConcurrentMap<String, Set<String>> _situationIdsByLineAndDirectionId = new ConcurrentHashMap<String, Set<String>>();

  private File _path;

  public void setPath(File path) {
    _path = path;
  }

  @PostConstruct
  public void start() {
    loadServieAlerts();
  }

  @PreDestroy
  public void stop() {
    saveServiceAlerts();
  }

  /****
   * {@link ServiceAlertsService} Interface
   ****/

  @Override
  public SituationBean createServiceAlert(String agencyId,
      SituationBean situation) {

    String id = agencyId + "_" + System.currentTimeMillis();
    situation.setId(id);

    if (situation.getCreationTime() == 0)
      situation.setCreationTime(System.currentTimeMillis());

    updateReferences(situation);
    saveServiceAlerts();

    return situation;
  }

  @Override
  public void updateServiceAlert(SituationBean situation) {
    updateReferences(situation);
    saveServiceAlerts();
  }

  @Override
  public void updateServiceAlerts(SituationExchangeDeliveryBean alerts) {

    List<SituationBean> situations = alerts.getSituations();

    if (situations == null || situations.isEmpty())
      return;

    for (SituationBean situation : situations) {
      updateReferences(situation);
    }

    saveServiceAlerts();
  }

  public void removeServiceAlert(String situationId) {

    SituationBean existingSituation = _situations.remove(situationId);

    if (existingSituation != null) {
      updateLineReferences(existingSituation, null);
      updateLineAndDirectionReferences(existingSituation, null);
    }
  }

  @Override
  public SituationBean getServiceAlertForId(String situationId) {
    return _situations.get(situationId);
  }

  @Override
  public ListBean<SituationBean> getServiceAlerts(SituationQueryBean query) {
    List<SituationBean> situations = new ArrayList<SituationBean>(
        _situations.values());
    return new ListBean<SituationBean>(situations, false);
  }

  @Override
  public List<SituationBean> getSituationsForLineId(String lineId) {

    List<String> situationIds = new ArrayList<String>();
    getSituationsForLineId(lineId, situationIds);
    return getSituationIdsAsBeans(situationIds);
  }

  @Override
  public List<SituationBean> getSituationsForLineId(String lineId,
      String directionId) {

    Set<String> situationIds = new HashSet<String>();
    getSituationsForLineId(lineId, situationIds);
    getSituationsForLineAndDirectionId(lineId, directionId, situationIds);
    return getSituationIdsAsBeans(situationIds);
  }

  /****
   * Private Methods
   ****/

  private void updateReferences(SituationBean situation) {

    String id = situation.getId();
    SituationBean existingSituation = _situations.put(id, situation);

    updateLineReferences(existingSituation, situation);
    updateLineAndDirectionReferences(existingSituation, situation);
  }

  private void updateLineReferences(SituationBean existingSituation,
      SituationBean situation) {

    Set<String> existingLineIds = getVehicleJourneysAsLineIds(existingSituation);
    Set<String> newLineIds = getVehicleJourneysAsLineIds(situation);

    for (String existingLineId : existingLineIds) {
      if (newLineIds.contains(existingLineId))
        continue;
      ConcurrentCollectionsLibrary.removeFromMapValueSet(_situationIdsByLineId,
          existingLineId, existingSituation.getId());
    }

    for (String newLineId : newLineIds) {
      if (existingLineIds.contains(newLineId))
        continue;
      ConcurrentCollectionsLibrary.addToMapValueSet(_situationIdsByLineId,
          newLineId, situation.getId());
    }
  }

  private void updateLineAndDirectionReferences(
      SituationBean existingSituation, SituationBean situation) {

    Set<String> existingLineIds = Collections.emptySet();
    if (existingSituation != null)
      existingLineIds = getVehicleJourneysAsLineAndDirectionIds(existingSituation);

    Set<String> newLineIds = getVehicleJourneysAsLineAndDirectionIds(situation);

    for (String existingLineId : existingLineIds) {
      if (newLineIds.contains(existingLineId))
        continue;
      ConcurrentCollectionsLibrary.removeFromMapValueSet(
          _situationIdsByLineAndDirectionId, existingLineId, existingSituation.getId());
    }

    for (String newLineId : newLineIds) {
      if (existingLineIds.contains(newLineId))
        continue;
      ConcurrentCollectionsLibrary.addToMapValueSet(
          _situationIdsByLineAndDirectionId, newLineId, situation.getId());
    }
  }

  private Set<String> getVehicleJourneysAsLineIds(SituationBean situation) {

    if (situation == null)
      return Collections.emptySet();

    Set<String> lineIds = new HashSet<String>();
    SituationAffectsBean affects = situation.getAffects();
    if (affects != null) {
      List<SituationAffectedVehicleJourneyBean> journeys = affects.getVehicleJourneys();
      if (journeys != null) {
        for (SituationAffectedVehicleJourneyBean journey : journeys) {
          lineIds.add(journey.getLineId());
        }
      }
    }
    return lineIds;
  }

  private Set<String> getVehicleJourneysAsLineAndDirectionIds(
      SituationBean situation) {

    if (situation == null)
      return Collections.emptySet();

    Set<String> lineIds = new HashSet<String>();
    SituationAffectsBean affects = situation.getAffects();
    if (affects != null) {
      List<SituationAffectedVehicleJourneyBean> journeys = affects.getVehicleJourneys();
      if (journeys != null) {
        for (SituationAffectedVehicleJourneyBean journey : journeys) {
          if (journey.getDirection() == null)
            continue;
          lineIds.add(journey.getLineId() + ID_SEPARATOR
              + journey.getDirection());
        }
      }
    }
    return lineIds;
  }

  private void getSituationsForLineId(String lineId,
      Collection<String> situationIds) {
    Set<String> ids = _situationIdsByLineId.get(lineId);
    if (ids != null)
      situationIds.addAll(ids);
  }

  private void getSituationsForLineAndDirectionId(String lineId,
      String directionId, Collection<String> situationIds) {
    String id = joinLineAndDirectionId(lineId, directionId);
    Set<String> ids = _situationIdsByLineId.get(id);
    if (ids != null)
      situationIds.addAll(ids);
  }

  private List<SituationBean> getSituationIdsAsBeans(
      Collection<String> situationIds) {
    List<SituationBean> situations = new ArrayList<SituationBean>(
        situationIds.size());
    for (String situationId : situationIds) {
      SituationBean situation = _situations.get(situationId);
      if (situation != null)
        situations.add(situation);
    }
    return situations;
  }

  private String joinLineAndDirectionId(String lineId, String directionId) {
    return lineId + ID_SEPARATOR + directionId;
  }

  /****
   * Serialization
   ****/

  private void loadServieAlerts() {
    if (_path == null || !_path.exists())
      return;

    try {

      List<SituationBean> situations = ObjectSerializationLibrary.readObject(_path);
      for (SituationBean situation : situations)
        updateReferences(situation);

    } catch (Exception ex) {
      _log.error("error loading service alerts from path " + _path, ex);
    }
  }

  private void saveServiceAlerts() {
    if (_path == null)
      return;

    try {
      List<SituationBean> situations = new ArrayList<SituationBean>(
          _situations.values());
      ObjectSerializationLibrary.writeObject(_path, situations);
    } catch (Exception ex) {
      _log.error("error saving service alerts to path " + _path, ex);
    }
  }
}
