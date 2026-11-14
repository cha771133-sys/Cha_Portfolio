package com.seongjin.portfolio.config;

import com.seongjin.portfolio.entity.Project;
import com.seongjin.portfolio.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 앱 시작 시 샘플 프로젝트 데이터를 넣어준다.
 * 실제 내용은 각자 프로젝트에 맞게 수정해서 사용할 것.
 * 운영 배포 시에는 이 클래스를 지우고 관리자 페이지나 DB 직접 입력으로 대체하는 걸 권장.
 *
 * 카테고리는 "논문" → "학교활동" 순서로 노출된다 (displayOrder 기준, 처음 등장한 순서 유지).
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProjectRepository projectRepository;

    @Override
    public void run(String... args) {
        if (projectRepository.count() > 0) {
            return;
        }

        // ===== 논문 =====

        // 1. 2026 2학기 소논문 - 트래픽 부하 상황에서 Gateway 성능 비교
        projectRepository.save(Project.builder()
                .category("논문")
                .title("API Gateway 성능 비교 연구 (소논문)")
                .summary("MSA 환경에서 Nginx(OpenResty), Kong, Spring Cloud Gateway를 JWT(RS256) 인증 및 트래픽 스파이크 상황에서 비교")
                .problem("""
                        MSA에서는 API Gateway가 모든 요청의 단일 진입점이라, Gateway의 성능이 시스템 전체의 지연과 안정성을 좌우합니다.

                        실무에서는 모든 요청에 JWT 서명 검증이 붙고, 수강신청이나 티켓 예매처럼 트래픽이 순간적으로 급증하는 상황도 빈번합니다. 그런데 기존 Gateway 비교 연구는 대부분 일정한 부하에서 라우팅 성능만 측정했을 뿐, JWT 인증 부하와 트래픽 스파이크를 동시에 반영한 비교는 부족했습니다.

                        그래서 두 조건(JWT 인증 부하 + 트래픽 스파이크)을 결합해 Gateway별 한계와 병목을 실증하는 것을 연구의 출발점으로 삼았습니다.
                        """)
                .role("""
                        단독 연구 - 실험 환경 구축, 실험 설계, 벤치마킹, 결과 분석
                        (백엔드 서버 2개 구현, Nginx용 JWT 검증 로직 직접 구현, Prometheus·Grafana 모니터링 환경 구성 포함)
                        """)
                .techDecision("""
                        비교 대상 3종을 서로 다른 동시성 모델의 대표 주자로 선정했습니다.
                        - Kong: 전문 API Gateway의 사실상 기준점(baseline)
                        - Nginx(OpenResty): Kong의 기반 엔진이자 가장 보편적인 웹 인프라, 싱글 이벤트 루프(비동기) 구조의 대표
                        - Spring Cloud Gateway: Spring 생태계 MSA의 표준, JVM 스레드 풀(동기) 구조의 대표

                        인증 방식은 JWT(RS256)로 정했습니다. RS256은 CPU 집약적인 서명 검증 연산이 필요해 Gateway 간 부하 차이를 가장 잘 드러낼 수 있다고 판단했습니다.

                        실험 환경은 Docker Compose로 3종을 동일 조건(컨테이너 메모리 1GB 통일)에 구성하고, k6로 Baseline → Spike → Recovery 구간의 부하를 생성했으며, Prometheus로 자원 사용량을 모니터링했습니다.
                        """)
                .troubleshooting("""
                        SCG 메모리 병목: 초기 컨테이너 메모리를 512MB로 통일했을 때 Spring Cloud Gateway의 JWT 검증 시 실패율과 지연이 급증했습니다. docker stats로 확인한 결과 메모리 사용률이 99%에 달하며 JVM GC 부담이 원인이었고, 메모리를 1GB로 재설정해 실패율 0%, p95 1.09s → 794ms로 개선했습니다.

                        Nginx JWT 검증 로직 직접 구현: lua-resty-jwt 라이브러리가 OpenSSL 3.x와 호환되지 않아, lua-resty-openssl 기반으로 JWT 검증 로직을 직접 구현했습니다.

                        Nginx는 자원 증설로 해결되지 않는 문제였습니다: 메모리를 늘려도 p95가 오히려 20.5s → 23.45s로 악화되어 "자원 부족" 가설을 기각했고, 이벤트 루프 블로킹이라는 구조적 원인을 새로운 가설로 세웠습니다.
                        """)
                .outcome("""
                        JWT 인증 적용 시 Gateway별 p95 응답시간/실패율/dropped 요청 수

                        - Kong: p95 1.9ms, 실패율 0%, dropped 0건
                        - Nginx(OpenResty): p95 23.45s, 실패율 0.08%, dropped 2585건
                        - Spring Cloud Gateway: p95 794.64ms, 실패율 0%, dropped 199건

                        Kong은 JWT 유무와 무관하게 가장 빠르고 스파이크 상황에도 안정적이었습니다.
                        Spring Cloud Gateway는 개별 요청 처리 속도는 느리지만(중앙값 142ms) 스파이크에는 안정적이었고, 병목은 자원 증설로 해결 가능했습니다.
                        Nginx(OpenResty)는 평상시엔 빠르지만 JWT 인증과 트래픽 스파이크가 결합되면 가장 불안정했습니다. RSA 서명 검증 연산이 단일 이벤트 루프를 블로킹하는 구조적 문제로 해석하고 있습니다.

                        단, 이 구조적 원인은 현재 가설 단계이며, CPU 사용률 직접 관측과 HS256(비RSA) 대조 실험이 후속 과제로 남아 있습니다.
                        """)
                .techStack(List.of("Nginx(OpenResty)", "Kong", "Spring Cloud Gateway", "Docker Compose",
                        "k6", "Prometheus", "Grafana", "JWT(RS256)"))
                .period("2026.09 - 진행중")
                .displayOrder(1)
                .build());

        // ===== 학교활동 =====

        // 2. GDGoC - 안심집
        projectRepository.save(Project.builder()
                .category("학교활동")
                .title("안심집")
                .summary("공공데이터 기반 1인 가구를 위한 관악구 동네 안전등급 분석 웹 서비스")
                .problem("""
                        1인 가구가 집을 구할 때 가격·교통 정보는 쉽게 확인하지만 "이 골목이 밤에 안전한가?"라는 질문에 답해주는 서비스는 없습니다.

                        CCTV·보안등·파출소·범죄 통계 데이터는 공공데이터포털, 경찰청, 행정안전부 등에 공개되어 있지만 부처별로 분산되어 있어 일반 사용자가 직접 조합해 활용하기는 어렵습니다. 이렇게 흩어진 공공데이터를 통합·점수화해 정보 비대칭을 해결하는 것이 프로젝트의 목표입니다.
                        """)
                .role("""
                        팀 5명(데이터 1, 백엔드 2, DB 1, 프론트 1) 중 데이터 담당.
                        공공데이터포털·서울 열린데이터광장에서 CCTV·보안등·파출소·강력범죄 통계 원시 데이터를 수집하고, 관악구 대상으로 정제·필터링한 뒤 4개 데이터셋을 공통 스키마로 통합하는 CSV 파이프라인을 설계했습니다.
                        """)
                .techDecision("진행 중인 프로젝트로 기술 스택 확정 전 (추후 업데이트 예정)")
                .troubleshooting("""
                        초기에는 서울시 가로등 데이터를 사용했는데, 관악구 범위에서 219개만 확보되어 실제 규모 대비 과소 집계되는 문제가 있었습니다. "서울특별시_관악구_보안등정보" 데이터셋으로 교체해 13,283개로 확대했습니다.

                        데이터 통합 후 cctv_pixel 컬럼은 99.7%가 동일값(200), install_ym 컬럼은 100% 결측으로 변별력이 없다는 것을 확인하고 제거했습니다.
                        """)
                .outcome("진행 중. 관악구 대상 통합 안전데이터 CSV 완성 - 총 15,399행 (CCTV 2,106 / 보안등 13,283 / 파출소 9 / 범죄통계 1)")
                .techStack(List.of("Python", "Pandas"))
                .githubUrl("https://github.com/fud21/2026_GDGoC_SCH")
                .period("2026.10 - 진행중")
                .displayOrder(2)
                .build());

        // 3. 멋사(Likelion) - 카공족을 위한 지도 (TrackLook, 성동구)
        projectRepository.save(Project.builder()
                .category("학교활동")
                .title("TrackLook (카공족을 위한 지도)")
                .summary("성동구 카공족(노트북 작업자·학생)을 위한 카페 추천 서비스 - 네이버 리뷰를 LLM으로 분석해 콘센트·와이파이·소음·눈치·테이블 5대 지표로 점수화 (Likelion 14기 팀 프로젝트)")
                .problem("""
                        카공족(노트북 작업자·학생)에게는 콘센트, 와이파이, 소음, 체류 편의성 등 카페별 작업 환경이 중요한데, 이런 정보는 블로그 리뷰 텍스트 곳곳에 흩어져 있어 하나하나 읽어보지 않으면 파악하기 어렵습니다. "카공하기 좋다"는 기준도 사람마다 달라, 리뷰를 읽어도 내게 맞는 카페인지 바로 판단하기 어려운 문제가 있었습니다.

                        이를 해결하기 위해 네이버 블로그 리뷰 텍스트를 LLM으로 분석해 콘센트·와이파이·소음·눈치·테이블 5개 지표로 세분화된 점수를 제공하고, 사용자가 원하는 조건으로 직접 필터링할 수 있게 만들었습니다.
                        """)
                .role("""
                        팀 3명(PM 1, 프론트엔드 1, 백엔드 1) 중 백엔드 담당.
                        로그인/인증(JWT) DB 처리, 위치 기반 카페 검색, 리뷰 작성·조회·삭제 API, 카페 목록 필터링·정렬 API를 구현했고, 발표용 시스템 아키텍처 자료를 작성했습니다.
                        """)
                .techDecision("""
                        Django REST Framework를 선택해 짧은 기간 안에 REST API를 빠르게 설계하고, djangorestframework-simplejwt로 JWT 인증을 손쉽게 구현했습니다.

                        LLM 점수화는 네이버 블로그 리뷰 텍스트를 Qwen3.5:122b(Ollama, NVIDIA L40 GPU)에 입력해 콘센트·와이파이·소음·눈치 4개 지표를 1~5점으로, 테이블 지표만 0점 또는 5점의 이진(binary)으로 채점하도록 설계했습니다. 테이블은 리뷰에 언급 자체가 없는 경우가 많아, 세분화된 점수보다 "확실한 긍정 신호가 있는지"만 판별하는 쪽이 더 신뢰도 높은 지표라고 판단했습니다. 채점 기준은 지표별 키워드 사전 형태로 문서화해 일관된 점수가 나오도록 했습니다.

                        데이터 파이프라인은 fetch_cafes(카카오 로컬 API로 카페 목록 수집) → crawl_reviews(네이버 블로그 리뷰 크롤링) → score_workspaces(LLM 채점) → enrich_cafes(카카오 API로 전화번호 등 추가 정보 수집) 순서의 Django management command로 설계했습니다.
                        """)
                .troubleshooting("""
                        지도에 카페 마커 258개가 한 번에 표시되어 화면이 복잡해지는 문제가 있었습니다. 팀 논의 후 우선순위 기준 상위 30개만 표시하도록 조정했습니다.

                        API 명세 초안에는 이메일 기반 회원가입이 포함돼 있었는데, 일정상 복잡도를 낮추기 위해 아이디·비밀번호만 사용하는 인증 구조로 변경했습니다.

                        사용자 위치 기반 검색 시 실제 위치(성동구)가 아닌 엉뚱한 지역으로 잡히는 문제가 있어, 발표·시연 화면에서는 위치값을 고정해 안정적으로 동작하도록 임시 조치했습니다. (실사용 환경에서 위치 정확도를 높이는 것은 후속 과제로 남아 있습니다)
                        """)
                .outcome("""
                        3주 내 백엔드 API(목록 조회·필터링·정렬·단일 조회, 회원가입·로그인·로그아웃, 리뷰 작성·조회·삭제)를 전부 구현해 발표·제출을 완료했습니다.
                        성동구 카페 258개 기본 데이터와 네이버 블로그 리뷰 3,693건을 수집해 LLM으로 5대 지표 점수화까지 마쳤습니다.
                        """)
                .techStack(List.of("Django 6.0", "Django REST Framework", "SQLite", "SimpleJWT",
                        "Kakao 로컬 API", "카카오맵 SDK", "Naver 블로그 검색 API", "Qwen3.5:122b (Ollama, L40 GPU)"))
                .githubUrl("https://github.com/sznii161/LikeLIon_")
                .period("2026.06 - 2026.07")
                .displayOrder(3)
                .build());

        // 4. 멋사(Likelion) 해커톤 - 다시 봄 (본선 진출)
        projectRepository.save(Project.builder()
                .category("학교활동")
                .title("다시 봄 (Likelion 해커톤)")
                .summary("완경기 여성의 흩어진 몸·마음·피부 신호를 AI로 연결해 생활습관으로 관리하는 헬스케어 서비스 - 순천향 Likelion 해커톤 1팀 \"얼룩덜룩\" (본선 진출)")
                .problem("""
                        완경(갱년기)에 접어든 50대 여성은 홍조·발한 같은 증상이 하루에도 여러 번 반복되지만, 각각이 "사건"으로만 느껴질 뿐 "데이터"로 쌓이지 않아 스스로도 패턴을 파악하기 어렵습니다. 가족에게 증상을 말해도 공감받지 못해 고립되고, "병원에 갈 정도로 심각한가"를 판단할 기준이 없어 비용을 따지기도 전에 병원행을 포기합니다. 게다가 디지털 기기에 낯선 경우가 많아, 앱을 설치해도 온보딩 단계에서 "이게 나를 위한 것인가"라는 자기 검열로 금방 이탈합니다.

                        이 문제를 풀기 위해 몸·마음·피부 데이터를 하나로 엮어 "오늘 컨디션이 왜 이런지" 설명해주고, 바쁜 일상 틈새에 자기 돌봄을 심어주는 서비스를 기획했습니다.
                        """)
                .role("""
                        팀 5명(풀스택 1, 프론트엔드 1, 챗봇 1, 얼굴 사진 분석 1, 증상 분석 1) 중 풀스택 담당.
                        복약·영양제, 명상·스트레칭 알림 도메인(Reminder/ReminderCompletion 모델, CRUD + 완료 토글 API)을 설계·구현했고, 데모 계정 가정으로 흩어져 있던 다른 앱들의 인증 구조를 JWT 기반 실제 로그인 사용자(request.user) 기준으로 통합하는 리팩토링을 담당했습니다.
                        """)
                .techDecision("""
                        Reminder API는 PUT을 막고 PATCH만 허용했습니다. 리마인더 수정은 대부분 시간·활성화 여부 같은 일부 필드만 바뀌는 경우라, 전체 교체보다 부분 수정이 요구사항에 더 맞다고 판단했습니다.

                        완료 처리는 별도의 boolean 필드 대신 "그날의 완료 기록(ReminderCompletion)이 있는지 여부"로 상태를 표현했습니다. 완료 기록이 있으면 삭제(=취소), 없으면 생성(=완료)하는 방식으로 설계해, 클라이언트가 현재 상태를 몰라도 같은 엔드포인트를 호출하는 것만으로 토글이 되도록 만들었습니다. 비활성화된 리마인더를 완료 처리하려는 요청은 404로 막아 데이터 정합성을 지켰습니다.

                        시연용 시드 데이터는 난수 시드를 고정해, 몇 번을 다시 생성해도 같은 패턴(기준일만 오늘로 이동)이 나오도록 만들었습니다. 발표 중 데이터가 매번 달라지면 리허설과 실제 시연 결과가 어긋날 수 있기 때문입니다.
                        """)
                .troubleshooting("""
                        인증 구조를 JWT + request.user 기반으로 리팩토링(`refactor/auth-integration`)하는 과정에서, 다른 팀원이 작성해둔 챗봇 백엔드 코드와 테스트가 기존 AllowAny + 데모 계정 가정으로 짜여 있다는 걸 미처 공유하지 못해, 리팩토링 이후 해당 테스트 8개가 전부 401로 깨지는 문제가 발생했습니다. 담당자가 force_authenticate로 테스트를 보완해 해결했지만, 이 일로 여러 사람이 동시에 건드리는 공통 구조(인증 등)를 바꿀 때는 변경 범위를 미리 공유해야 한다는 걸 체감했습니다.
                        """)
                .outcome("순천향 Likelion 해커톤 본선 진출. 5인 팀에서 알림 도메인과 전체 서비스의 인증 통합을 담당해, 팀원들이 작성한 기능들이 하나의 일관된 인증 체계 위에서 동작하도록 기여했습니다.")
                .techStack(List.of("Django", "Django REST Framework", "PostgreSQL", "React Native (Expo)", "Gemini API", "JWT"))
                .githubUrl("https://github.com/jsi4770/dasi-bom")
                .period("2026.08")
                .displayOrder(4)
                .build());

        // 5. 약속 (Promise) - 뤼튼 부트캠프
        projectRepository.save(Project.builder()
                .category("학교활동")
                .title("약속 (Promise)")
                .summary("시니어(60대 이상)를 위한 AI 복약 관리 시스템 - 처방전 사진 한 장으로 복약 알림을 자동 등록하고, 브라우저를 닫아도 환자·보호자에게 텔레그램 알림이 발송되는 서비스 (뤼튼 부트캠프 참가작)")
                .problem("""
                        고령층은 하루에 여러 종류의 약을 정해진 시각에 챙겨 먹어야 하지만, 복용 시간을 놓치거나 약을 혼동하기 쉽습니다. 기존 복약 알림 앱은 사용자가 약 이름과 복용 시각을 일일이 직접 입력해야 해서, 디지털 기기에 익숙하지 않은 시니어에게는 진입 장벽이 높았습니다.

                        또한 혼자 사는 어르신의 경우 알림을 놓쳤을 때 이를 확인해줄 사람이 없다는 문제도 있었습니다. 처방전 사진 한 장으로 복약 정보를 자동 등록하고, 보호자도 함께 알림을 받을 수 있게 만들어 이 두 가지 문제를 해결하고자 했습니다.
                        """)
                .role("""
                        개인 프로젝트 - 기획·설계·개발 전 과정 단독 수행 (뤼튼 부트캠프).
                        처방전 OCR 분석 파이프라인, 서버 사이드 스케줄 알림 아키텍처, 텔레그램 봇 연동, 보호자 정보 저장 구조를 모두 직접 설계·구현했습니다.
                        """)
                .techDecision("""
                        가장 핵심적인 설계 결정은 "브라우저를 닫아도 복약 시각에 알림이 가야 한다"는 요구사항을 어떻게 풀 것인가였습니다. 클라이언트(브라우저) 타이머에 의존하면 사용자가 앱을 끄는 순간 알림이 멈추기 때문에, 알림 스케줄링을 서버 사이드로 옮겨야 했습니다.

                        별도의 상시 가동 서버를 두는 대신 Upstash QStash(서버리스 cron)를 선택했습니다. 처방전 분석이 끝나면 약별 복약 시각마다 QStash에 cron 작업을 등록하고, 지정된 시각이 되면 QStash가 /api/notify 엔드포인트를 호출해 텔레그램 알림을 발송하는 구조입니다. 이렇게 하면 Vercel 서버리스 환경에서도 상시 서버 없이 정시 알림이 가능하고, 사용자의 브라우저 상태와 완전히 독립적으로 동작합니다.

                        처방전 인식은 Google Gemini 2.5 Flash의 Vision 기능으로 OCR과 약품 정보 추출을 한 번에 처리했고, 추출된 약품 정보는 식약처 공공 API로 교차 검증했습니다. 보호자 정보는 영구 저장이 필요 없는 휘발성 데이터라, 30일 TTL을 걸어 Upstash Redis에 저장했습니다.
                        """)
                .troubleshooting("""
                        QStash는 외부에서 접근 가능한 공개 URL만 호출할 수 있어, 로컬 개발 환경(localhost)에서는 QStash가 /api/notify를 직접 호출하는 흐름을 테스트할 수 없었습니다. 알림 발송 엔드포인트에는 QStash 서명 검증 로직이 있는데, 로컬에서는 이 서명 검증이 실패합니다.

                        이를 해결하기 위해 로컬에서는 /api/notify에 직접 POST 요청을 보내 발송 로직 자체를 검증하고, QStash 연동을 포함한 전체 흐름은 Vercel 배포 환경에서 확인하는 방식으로 테스트 전략을 나눴습니다.
                        """)
                .outcome("""
                        Vercel에 배포해 실제 동작하는 서비스로 완성했습니다. 처방전 업로드 → OCR 분석 → 복약 스케줄 자동 등록 → 정시 텔레그램 알림(환자+보호자)의 전체 흐름이 브라우저 종료와 무관하게 동작합니다.
                        버튼 최소 높이 60px, 폰트 최소 18px, 음성 안내 등 시니어 친화 UX 가이드라인을 적용했습니다.
                        """)
                .techStack(List.of("Next.js 16", "TypeScript", "Tailwind CSS v4", "Gemini 2.5 Flash (Vision)",
                        "Upstash QStash", "Upstash Redis", "Telegram Bot API", "식약처 공공 API", "Vercel"))
                .githubUrl("https://github.com/cha771133-sys/Personal-Project")
                .demoUrl("https://personal-project-yctb.vercel.app")
                .period("2026.09 - 진행중")
                .displayOrder(5)
                .build());
    }

}
