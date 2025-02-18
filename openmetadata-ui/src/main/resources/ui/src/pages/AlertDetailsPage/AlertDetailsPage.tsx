/*
 *  Copyright 2022 Collate.
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

import { Card } from 'antd';
import { trim } from 'lodash';
import React, { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import { AlertDetailsComponent } from '../../components/Alerts/AlertsDetails/AlertDetails.component';
import DeleteWidgetModal from '../../components/common/DeleteWidget/DeleteWidgetModal';
import PageLayoutV1 from '../../components/PageLayoutV1/PageLayoutV1';
import { GlobalSettingsMenuCategory } from '../../constants/GlobalSettings.constants';
import { EntityType } from '../../enums/entity.enum';
import {
  EventFilterRule,
  EventSubscription,
  FilteringRules,
} from '../../generated/events/eventSubscription';
import { getAlertsFromId } from '../../rest/alertsAPI';
import { getEntityName } from '../../utils/EntityUtils';
import { getSettingPath } from '../../utils/RouterUtils';
import { showErrorToast } from '../../utils/ToastUtils';

const AlertDetailsPage = () => {
  const { t } = useTranslation();

  const { fqn: id } = useParams<{ fqn: string }>();
  const [loadingCount, setLoadingCount] = useState(0);
  const [alerts, setAlerts] = useState<EventSubscription>();
  const [showDeleteModel, setShowDeleteModel] = useState(false);

  const fetchAlert = async () => {
    try {
      setLoadingCount((count) => count + 1);

      const response: EventSubscription = await getAlertsFromId(id);

      const requestFilteringRules =
        response.filteringRules?.rules?.map((curr) => {
          const [fullyQualifiedName, filterRule] =
            curr.condition?.split('(') ?? [];

          return {
            ...curr,
            fullyQualifiedName,
            condition: filterRule
              .replaceAll("'", '')
              .replace(new RegExp(`\\)`), '')
              .split(',')
              .map(trim),
          } as unknown as EventFilterRule;
        }) ?? [];

      setAlerts({
        ...response,
        filteringRules: {
          ...(response.filteringRules as FilteringRules),
          rules: requestFilteringRules,
        },
      });
    } catch {
      showErrorToast(
        t('server.entity-fetch-error', { entity: t('label.alert') }),
        id
      );
    } finally {
      setLoadingCount((count) => count - 1);
    }
  };

  useEffect(() => {
    if (id) {
      fetchAlert();
    }
  }, [id]);

  const breadcrumb = useMemo(
    () => [
      {
        name: t('label.notification-plural'),
        url: getSettingPath(GlobalSettingsMenuCategory.NOTIFICATIONS),
      },
      {
        name: getEntityName(alerts),
        url: '',
      },
    ],
    [alerts]
  );

  return (
    <PageLayoutV1 pageTitle={getEntityName(alerts)}>
      {loadingCount > 0 && <Card loading={loadingCount > 0} />}
      {alerts && (
        <AlertDetailsComponent
          alerts={alerts}
          breadcrumb={breadcrumb}
          onDelete={() => setShowDeleteModel(true)}
        />
      )}
      <DeleteWidgetModal
        afterDeleteAction={() => history.back()}
        allowSoftDelete={false}
        entityId={alerts?.id || ''}
        entityName={getEntityName(alerts)}
        entityType={EntityType.SUBSCRIPTION}
        visible={showDeleteModel}
        onCancel={() => {
          setShowDeleteModel(false);
        }}
      />
    </PageLayoutV1>
  );
};

export default AlertDetailsPage;
