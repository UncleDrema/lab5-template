# RSOI Universal Helm Chart

Универсальный Helm чарт для развёртывания микросервисов приложения RSOI.

## Структура

```
charts/rsoi/
├── Chart.yaml           # Метаданные чарта
├── values.yaml          # Базовые значения (template)
├── values-gateway.yaml      # Значения для Gateway
├── values-flights.yaml      # Значения для Flights
├── values-tickets.yaml      # Значения для Tickets
├── values-privileges.yaml   # Значения для Privileges
└── templates/           # Kubernetes шаблоны
    ├── deployment.yaml
    ├── service.yaml
    ├── ingress.yaml
    ├── serviceaccount.yaml
    └── _helpers.tpl
```

## Использование

### Развёртывание БД (PostgreSQL)

**Обязательно запусти PostgreSQL первым!**

```bash
helm install postgres ./charts/postgres --namespace rsoi --create-namespace
```

Проверь статус:
```bash
kubectl get statefulset postgres -n rsoi
```

### Развёртывание микросервисов

После успешного запуска PostgreSQL развёртывай микросервисы.

#### Развёртывание одного сервиса

**Gateway (с ingress для входа в приложение):**
```bash
helm install gateway ./charts/rsoi -f ./charts/rsoi/values-gateway.yaml --namespace rsoi --create-namespace
```

**Flights:**
```bash
helm install flights ./charts/rsoi -f ./charts/rsoi/values-flights.yaml --namespace rsoi
```

**Tickets:**
```bash
helm install tickets ./charts/rsoi -f ./charts/rsoi/values-tickets.yaml --namespace rsoi
```

**Privileges:**
```bash
helm install privileges ./charts/rsoi -f ./charts/rsoi/values-privileges.yaml --namespace rsoi
```

### Развёртывание всех сервисов сразу

Создай файл `values-all.yaml`:
```yaml
gateway:
  enabled: true
flights:
  enabled: true
tickets:
  enabled: true
privileges:
  enabled: true
```

Затем развёртывай каждый сервис по очереди (как выше).

### Обновление чарта

```bash
helm upgrade <release-name> ./charts/rsoi -f ./charts/rsoi/values-<service>.yaml --namespace rsoi
```

```
helm upgrade gateway ./charts/rsoi -f ./charts/rsoi/values-gateway.yaml --namespace rsoi
```

### Полная развёртка всех компонентов

```bash
# 1. PostgreSQL
helm install postgres ./charts/postgres --namespace rsoi --create-namespace

# 2. Ожидай, пока БД будет готова
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=postgres -n rsoi --timeout=300s

# 3. Микросервисы
helm install gateway ./charts/rsoi -f ./charts/rsoi/values-gateway.yaml --namespace rsoi
helm install flights ./charts/rsoi -f ./charts/rsoi/values-flights.yaml --namespace rsoi
helm install tickets ./charts/rsoi -f ./charts/rsoi/values-tickets.yaml --namespace rsoi
helm install privileges ./charts/rsoi -f ./charts/rsoi/values-privileges.yaml --namespace rsoi
```

### Удаление

```bash
helm uninstall <release-name> --namespace rsoi
```

## Значения для каждого сервиса

Каждый файл `values-<service>.yaml` содержит:

- **image**: Repository, pullPolicy, tag
- **service**: Тип сервиса, порт, имя
- **env**: Переменные окружения для приложения
- **ingress**: Настройки ingress (включен только для gateway)
- **resources**: Лимиты CPU/памяти
- **probes**: Liveness и readiness проверки

## Порты сервисов

- **Gateway**: 8080 (точка входа)
- **Flights**: 8060
- **Tickets**: 8070
- **Privileges**: 8050

## Примечания

- Все сервисы развёртываются в namespace `rsoi`
- Только gateway имеет ingress по умолчанию
- Сервисы обнаруживают друг друга по DNS-имену (например, `http://flights:8060`)
- Image repository указан на Yandex Cloud Registry
- **PostgreSQL должна быть запущена перед микросервисами**

## Подключение к БД

Из микросервисов подключайтесь к PostgreSQL:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/flights
SPRING_DATASOURCE_USERNAME=program
SPRING_DATASOURCE_PASSWORD=test
```

Или используй переменные окружения в values файлах сервисов.

## Помощь

- Логи PostgreSQL: `kubectl logs postgres-0 -n rsoi -f`
- Логи микросервиса: `kubectl logs <pod-name> -n rsoi -f`
- Все релизы: `helm list -n rsoi`
- Удаление всего: `helm uninstall postgres gateway flights tickets privileges -n rsoi`



## Установка и развертывание

### Kubernetes + Helm

#### Настройка секретов для Auth0

Проект использует Auth0 для OAuth2 авторизации. Учетные данные хранятся в Kubernetes Secrets и НЕ должны коммиться в Git.

1. **Создай файл с секретами:**
   ```bash
   cp charts/rsoi/secrets-gateway.yaml.example charts/rsoi/secrets-gateway.yaml
   ```

2. **Заполни свои Auth0 credentials в `charts/rsoi/secrets-gateway.yaml`:**
   ```yaml
   auth:
     auth0:
       clientId: "YOUR_AUTH0_CLIENT_ID"
       clientSecret: "YOUR_AUTH0_CLIENT_SECRET"
   ```

3. **Установи Helm чарт с секретами:**
   ```bash
   helm install gateway ./charts/rsoi \
     -f charts/rsoi/values-gateway.yaml \
     -f charts/rsoi/secrets-gateway.yaml
   ```

   Или для разных сервисов:
   ```bash
   helm install gateway ./charts/rsoi \
     -f charts/rsoi/values-gateway.yaml \
     -f charts/rsoi/secrets-gateway.yaml
   
   helm install flights ./charts/rsoi \
     -f charts/rsoi/values-flights.yaml
   
   helm install tickets ./charts/rsoi \
     -f charts/rsoi/values-tickets.yaml
   
   helm install privileges ./charts/rsoi \
     -f charts/rsoi/values-privileges.yaml
   ```

⚠️ **Важно:** Файл `secrets-gateway.yaml` находится в `.gitignore` и не должен коммиться в Git!
