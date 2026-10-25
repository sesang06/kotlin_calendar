## 요약

<!-- 이 PR이 무엇을, 왜 바꾸는지 2~3문장 -->

## 피처 / 범위

- 브랜치:
- 영역: proto / server / client / ci / build / agents
- 의존: <!-- stacked면 선행 PR 링크, 아니면 "없음" -->

## 변경 사항

-

## 검증

<!-- 실제로 실행한 명령과 결과. 실행하지 못한 것은 이유와 함께 -->

- [ ] `./gradlew ktlintCheck`
- [ ] 테스트:
- 검증하지 못한 항목:

## 리뷰 포인트

<!-- 리뷰어가 특히 봐야 할 곳, 고민한 트레이드오프 -->

## 체크리스트

- [ ] proto 변경 시 하위 호환 규칙 준수 (`proto-contract`)
- [ ] 계층 의존 방향 준수 (Handler → Service → Repository / MVVM)
- [ ] 새 기능에 테스트 추가
- [ ] 비밀값·접속 정보 미포함
