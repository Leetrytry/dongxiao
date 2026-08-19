package com.dongxiao.practice.practice;

public enum PracticeMode {
    LONG_TONE(
            "长音",
            "保持一个目标音，尽量让指针靠近 0 cent。建议每个音稳定 8 到 12 秒。"
    ),
    SCALE(
            "音阶",
            "从当前目标音开始，上行八音后再下行。每个音命中后会自动推进，重点练换指落点和音阶连贯性。"
    ),
    TONGUING(
            "吐音",
            "固定一个目标音做短促重复起音。系统会看起音次数、速度和均匀度，重点是每次吐完都回到本音。"
    ),
    VIBRATO(
            "气震音",
            "在稳定长音上加入有规律的气震。系统会看频率、深度和规律性，避免变成随机抖动。"
    ),
    SLIDE(
            "滑音",
            "从目标音上下方滑入目标音。系统会看滑动幅度、平滑度和落点，重点是滑入后能停稳。"
    ),
    ORNAMENT(
            "打音/叠音",
            "围绕本音做短促离音再回落。系统会看回落次数、离音幅度和回到本音的速度。"
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
