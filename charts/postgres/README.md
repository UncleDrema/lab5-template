# PostgreSQL Helm Chart

Helm чарт для развёртывания PostgreSQL 15 в Kubernetes для приложения RSOI.

## Структура

```
charts/postgres/
├── Chart.yaml                           # Метаданные чарта
├── values.yaml                          # Значения конфигурации
├── README.md                            # Этот файл
└── templates/
    ├── statefulset.yaml                 # StatefulSet для PostgreSQL
    ├── service.yaml                     # Kubernetes Service
    ├── secret.yaml                      # Secret с паролем
    ├── configmap-init-scripts.yaml     # ConfigMap с инит скриптами
    └── _helpers.tpl                     # Helpers для шаблонов
```

## Использование

### Установка PostgreSQL

```bash
helm install postgres ./charts/postgres --namespace rsoi --create-namespace
```

### С пользовательскими значениями

Создай файл `values-prod.yaml` и запусти:

```bash
helm install postgres ./charts/postgres -f values-prod.yaml --namespace rsoi
```

### Обновление

```bash
helm upgrade postgres ./charts/postgres --namespace rsoi
```

### Удаление

```bash
helm uninstall postgres --namespace rsoi
```

## Конфигурация

### Основные параметры в `values.yaml`:

```yaml
postgresql:
  username: "postgres"      # Пользователь суперадмина
  password: "postgres"      # Пароль суперадмина
  database: "postgres"      # База данных по умолчанию
  initScriptsConfigMap: "postgres-init-scripts"  # Имя ConfigMap с инит скриптами

persistence:
  enabled: true             # Включить persistent volume
  size: 10Gi                # Размер диска
  storageClass: "standard"  # Класс хранилища
```

## Инициализация БД

Чарт автоматически создаёт:

1. **program** пользователя с паролем `test`
2. Три базы данных: `flights`, `tickets`, `privileges`
3. Выдаёт все привилегии пользователю `program`

Скрипты инициализации находятся в `configmap-init-scripts.yaml`. Для изменения инициализации отредактируй ConfigMap в шаблоне.

## Подключение к БД из подов

Внутри кластера подключись по адресу:

```
postgresql://program:test@postgres:5432/flights
postgresql://program:test@postgres:5432/tickets
postgresql://program:test@postgres:5432/privileges
```

Или через переменные окружения в values микросервисов:

```yaml
env:
  - name: SPRING_DATASOURCE_URL
    value: "jdbc:postgresql://postgres:5432/flights"
  - name: SPRING_DATASOURCE_USERNAME
    value: "program"
  - name: SPRING_DATASOURCE_PASSWORD
    value: "test"
```

## Статус и логи

### Проверить статус

```bash
kubectl get statefulset postgres -n rsoi
kubectl get pod postgres-0 -n rsoi
```

### Логи PostgreSQL

```bash
kubectl logs postgres-0 -n rsoi -f
```

### Подключиться к БД напрямую

```bash
kubectl exec -it postgres-0 -n rsoi -- psql -U postgres
```

## Persistent Volume

Чарт использует `StatefulSet` с `VolumeClaimTemplate` для автоматического создания PVC. Данные БД сохраняются даже при перезагрузке пода.

Для удаления данных:

```bash
kubectl delete pvc postgres-data-postgres-0 -n rsoi
```

## Примечания

- PostgreSQL слушает на порту **5432**
- Инициализация БД происходит автоматически при первом запуске
- Используется StorageClass `standard` (зависит от кластера Kubernetes)
- Проверки liveness и readiness используют команду `pg_isready`
