## 1. 카프카 설치
   ```brew install kafka```

### 설치 확인
```
kafka-topics --version
4.0
```

## 2. KRaft 전용 설정 파일 준비
### KRaft 전용 설정 (단일 노드)

```
process.roles=broker,controller
node.id=1
```

### 리스너 정의
```
controller.listener.names=CONTROLLER
listeners=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
advertised.listeners=PLAINTEXT://localhost:9092
inter.broker.listener.name=PLAINTEXT
```

### 컨트롤러 쿼럼 구성
```
controller.quorum.voters=1@localhost:9093
```

### 로그 저장 경로
```
log.dirs=/usr/local/etc/kafka/server.properties
```

## 3. 클러스터 ID 생성 + 스토리지 포맷
```
kafka-storage random-uuid
예시 uuid: v0AbcdEfGhIjKlMnOpQrSt
```

### 위 UUID를 넣어서 포맷
```
kafka-storage format --config /usr/local/etc/kafka/server.properties \
--cluster-id v0AbcdEfGhIjKlMnOpQrSt
```

## 4. Kafka 실행
```
kafka-server-start /usr/local/etc/kafka/server.properties
```

## 5. 토픽/메시지 테스트
   새 터미널 열어서 실행
### 토픽 생성
```
kafka-topics --create --topic demo --bootstrap-server localhost:9092 \
--partitions 1 --replication-factor 1
```

### 토픽 확인
```
kafka-topics --list --bootstrap-server localhost:9092
```

### 프로듀서 실행
```
kafka-console-producer --topic demo --bootstrap-server localhost:9092
> topic   
> message
```

### 컨슈머 실행 (다른 터미널)
```
kafka-console-consumer --topic demo --bootstrap-server localhost:9092 --from-beginning
```

## 6. 상태 점검(KRaft 전용)
```
kafka-metadata-quorum --bootstrap-server localhost:9092 describe --status   
```
→ 정상이라면 컨트롤러 1개가 leader로 표시됨

```
``% kafka-metadata-quorum --bootstrap-server localhost:9092 describe --status
ClusterId:              v0AbcdEfGhIjKlMnOpQrSt
LeaderId:               1
LeaderEpoch:            3
HighWatermark:          2357
MaxFollowerLag:         0
MaxFollowerLagTimeMs:   0
CurrentVoters:          [{"id": 1, "directoryId": null, "endpoints": ["CONTROLLER://localhost:9093"]}]
CurrentObservers:       []
```
