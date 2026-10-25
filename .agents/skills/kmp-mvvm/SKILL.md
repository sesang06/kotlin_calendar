---
name: kmp-mvvm
description: kotlin_calendar 클라이언트 공통 로직의 MVVM 패턴. client/shared의 presentation 계층에서 화면(Screen), UiState, Action, Effect, ViewModel을 만들거나 고칠 때 사용. 새 화면/기능 추가, 상태 관리, 네비게이션 이벤트 처리 시 사용.
---

# KMP MVVM

## 파일 배치

```
client/shared/src/commonMain/kotlin/com/kotlincalendar/client/presentation/<feature>/
├── <Feature>UiState.kt   # 불변 data class 하나
├── <Feature>Action.kt    # 사용자 의도 sealed interface
├── <Feature>Effect.kt    # 1회성 이벤트 sealed interface (네비게이션, 스낵바)
├── <Feature>ViewModel.kt
└── <Feature>Screen.kt    # Route + Screen Composable
```

## 규칙

1. **단방향 흐름**: Screen → `onAction(Action)` → ViewModel → `StateFlow<UiState>` → Screen.
2. **UiState는 data class 하나.** 로딩·에러·데이터를 필드로. `sealed` 상태 분기보다 필드 조합을 우선한다.
3. **ViewModel은 Compose·플랫폼 API를 import하지 않는다.** `domain` 인터페이스에만 의존한다.
4. ViewModel은 KMP 공식 `androidx.lifecycle.ViewModel`을 상속하고 `viewModelScope`를 쓴다.
5. **1회성 이벤트**는 `Channel<Effect>(Channel.BUFFERED).receiveAsFlow()`. UiState에 넣지 않는다.
6. **Screen 분리**: `FeatureRoute`(ViewModel 주입·상태 수집·Effect 처리)와 `FeatureScreen(state, onAction)`(순수 UI).
7. 공개 API는 `state: StateFlow<UiState>`, `effects: Flow<Effect>`, `onAction(Action)` 세 개만.
8. 시간 의존 로직은 `Clock`을 생성자로 주입받는다 (테스트 가능하게).

## 템플릿

```kotlin
data class CalendarUiState(
    val selectedDate: LocalDate,
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface CalendarAction {
    data class SelectDate(val date: LocalDate) : CalendarAction
    data class OpenEvent(val id: String) : CalendarAction
    data object Retry : CalendarAction
}

sealed interface CalendarEffect {
    data class NavigateToEvent(val id: String) : CalendarEffect
}
```

```kotlin
class CalendarViewModel(
    private val eventRepository: EventRepository,
    clock: Clock = Clock.System,
) : ViewModel() {

    private val _state = MutableStateFlow(
        CalendarUiState(selectedDate = clock.todayIn(TimeZone.currentSystemDefault()))
    )
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    private val _effects = Channel<CalendarEffect>(Channel.BUFFERED)
    val effects: Flow<CalendarEffect> = _effects.receiveAsFlow()

    init { load() }

    fun onAction(action: CalendarAction) {
        when (action) {
            is CalendarAction.SelectDate -> {
                _state.update { it.copy(selectedDate = action.date) }
                load()
            }
            is CalendarAction.OpenEvent ->
                viewModelScope.launch { _effects.send(CalendarEffect.NavigateToEvent(action.id)) }
            CalendarAction.Retry -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            eventRepository.getEvents(_state.value.selectedDate)
                .onSuccess { events -> _state.update { it.copy(events = events, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, errorMessage = e.message) } }
        }
    }
}
```

```kotlin
@Composable
fun CalendarRoute(
    onNavigateToEvent: (String) -> Unit,
    viewModel: CalendarViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CalendarEffect.NavigateToEvent -> onNavigateToEvent(effect.id)
            }
        }
    }
    CalendarScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun CalendarScreen(state: CalendarUiState, onAction: (CalendarAction) -> Unit) {
    // 순수 UI. ViewModel, Koin 참조 금지.
}
```

## 새 화면 체크리스트

1. UiState / Action / Effect / ViewModel / Screen 작성
2. Koin에 `viewModelOf(::FeatureViewModel)` 등록 (`kmp-data-layer`)
3. `App.kt` NavHost에 `composable<FeatureRoute> { FeatureRoute(...) }` 추가 (type-safe route, `@Serializable`)
4. ViewModel 테스트 작성 (`kmp-testing`)
