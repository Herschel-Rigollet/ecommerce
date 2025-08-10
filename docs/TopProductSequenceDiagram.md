```mermaid
sequenceDiagram
    participant User
    participant ProductController
    participant StatsService
    participant OrderHistoryRepository

    %% 인기 상품 조회 흐름
    User->>ProductController: 인기 상품 목록 조회 요청
    ProductController->>StatsService: 3일 간 가장 많이 판매된 5개 상품 목록 조회 호출
    StatsService->>OrderHistoryRepository: 3일 동안의 top 5개 상품 목록 찾기
    OrderHistoryRepository-->>StatsService: 인기 상품 목록 반환
    StatsService-->>ProductController: 인기 상품 목록 반환
    ProductController-->>User: 인기 상품 목록

```