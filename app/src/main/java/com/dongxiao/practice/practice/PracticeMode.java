package com.dongxiao.practice.practice;

public enum PracticeMode {
    LONG_TONE(
            "长音",
            "保持一个目标音，尽量让指针靠近 0 cent。建议每个音稳定 8 到 12 秒。"
    ),
    SCALE(
            "音阶",
            "按目标音阶逐音练习。可开启自动匹配，观察每个音的偏差和换指后的稳定速度。"
    ),
    TONGUING(
            "吐音",
            "用短促而干净的起音重复目标音。指标会统计明显起音次数，注意不要用气过猛。"
    ),
    VIBRATO(
            "气震音",
            "在稳定长音上加入均匀气震。参考指标为气震频率和深度，先求均匀，再求幅度。"
    ),
    SLIDE(
            "滑音",
            "从邻近音滑向目标音，观察最近 1.5 秒的音高运动方向和最终落点。"
    ),
    ORNAMENT(
            "打音/叠音",
            "围绕本音做短促装饰动作。指标会统计快速音高波动次数，重点听本音是否被破坏。"
    );

    public final String label;
    public final String instruction;

    PracticeMode(String label, String instruction) {
        this.label = label;
        this.instruction = instruction;
    }

    @Override
    public String toString() {
        return label;
    }
}
