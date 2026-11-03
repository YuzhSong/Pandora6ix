package com.pandora6ix.backend.controller;

import com.pandora6ix.backend.api.Contracts;
import com.pandora6ix.backend.service.IdentityService;
import com.pandora6ix.backend.service.ai.AiService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

/** AI 探索馆：只读生成接口，不写任何业务数据；DeepSeek 不可用时返回兜底内容 */
@RestController
@RequestMapping("/api/v1")
public class AiController {
    private final IdentityService identity;
    private final AiService ai;

    public AiController(IdentityService identity, AiService ai) {
        this.identity = identity;
        this.ai = ai;
    }

    /** 今日幸运签；星座只用于当次生成，不保存 */
    @GetMapping("/ai/fortune")
    public Object fortune(@RequestParam(required = false) String zodiac) {
        return ai.fortune(identity.current(), zodiac);
    }

    @GetMapping("/ai/personality/questions")
    public Object personalityQuestions() {
        return ai.personalityQuestions(identity.current());
    }

    @PostMapping("/ai/personality")
    public Object personality(@Valid @RequestBody Contracts.PersonalityAnswers c) {
        return ai.personality(identity.current(), c);
    }

    @PostMapping("/ai/tarot")
    public Object tarot(@Valid @RequestBody Contracts.TarotDraw c) {
        return ai.tarot(identity.current(), c);
    }

    /** 本人近 30 天日志与本人任务的脱敏摘要 */
    @PostMapping("/ai/career-map")
    public Object careerMap() {
        return ai.careerMap(identity.current());
    }

    /** 现有可见范围内的公司重要事项与任务统计 */
    @PostMapping("/ai/company-outlook")
    public Object companyOutlook() {
        return ai.companyOutlook(identity.current());
    }
}
