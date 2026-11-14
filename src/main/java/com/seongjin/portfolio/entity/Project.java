package com.seongjin.portfolio.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 프로젝트명 */
    @Column(nullable = false, length = 100)
    private String title;

    /** 카드에 보일 한 줄 요약 */
    @Column(nullable = false, length = 200)
    private String summary;

    /** 문제 정의 - 왜 만들었는지 */
    @Lob
    private String problem;

    /** 담당 역할 */
    @Lob
    private String role;

    /** 기술적 의사결정 - 왜 이 기술을 썼는지 */
    @Lob
    private String techDecision;

    /** 트러블슈팅 - 막혔던 문제와 해결 과정 */
    @Lob
    private String troubleshooting;

    /** 성과 / 결과 */
    @Lob
    private String outcome;

    /** 기술 스택 태그 */
    @ElementCollection
    @CollectionTable(name = "project_tech_stack", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "tech")
    private List<String> techStack = new ArrayList<>();

    private String githubUrl;

    private String demoUrl;

    /** 진행 기간 (예: "2026.03 - 진행중") */
    private String period;

    /** 카드 정렬 순서 (낮을수록 먼저) */
    @Column(nullable = false)
    private Integer displayOrder;

    /** 분야 묶음 (예: "논문", "학교활동") */
    @Column(nullable = false, length = 50)
    private String category;

    @Builder
    public Project(String title, String summary, String problem, String role,
                    String techDecision, String troubleshooting, String outcome,
                    List<String> techStack, String githubUrl, String demoUrl,
                    String period, Integer displayOrder, String category) {
        this.title = title;
        this.summary = summary;
        this.problem = problem;
        this.role = role;
        this.techDecision = techDecision;
        this.troubleshooting = troubleshooting;
        this.outcome = outcome;
        this.techStack = techStack != null ? techStack : new ArrayList<>();
        this.githubUrl = githubUrl;
        this.demoUrl = demoUrl;
        this.period = period;
        this.displayOrder = displayOrder;
        this.category = category;
    }
}
