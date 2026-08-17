package com.dongxiao.practice.song;

import java.util.Arrays;
import java.util.List;

public final class JianpuTextCatalog {
    private static final String BASE = "jianpu_text/";

    private JianpuTextCatalog() {
    }

    public static List<JianpuTextSource> defaults() {
        return Arrays.asList(
                source("G弦上的咏叹调（小图）", "G", 90, "score_01_g_string_aria_square.txt"),
                source("G弦上的咏叹调", "G", 90, "score_02_g_string_aria.txt"),
                source("红颜旧", "F", 76, "score_03_hong_yan_jiu.txt"),
                source("你离开了南京，从此没有人和我说话", "F", 61, "score_04_nanjing.txt"),
                source("一步之遥", "G", 120, "score_05_por_una_cabeza.txt"),
                source("断桥残雪", "G", 80, "score_06_duan_qiao_can_xue.txt"),
                source("天宫舞曲", "G", 90, "score_07_tian_gong_wu_qu.txt"),
                new JianpuTextSource(
                        "琵琶行",
                        "A",
                        92,
                        4,
                        BASE + "score_08_pipa_xing_p1.txt",
                        BASE + "score_08_pipa_xing_p2.txt"
                ),
                source("莫失莫忘", "F#", 72, "score_09_mo_shi_mo_wang.txt"),
                source("长相思", "Bb", 72, "score_10_chang_xiang_si.txt"),
                source("倩女幽魂", "F", 110, "score_11_qian_nv_you_hun.txt"),
                source("卷珠帘", "C", 76, "score_12_juan_zhu_lian.txt"),
                source("入殓师", "C", 78, "score_13_ru_lian_shi.txt"),
                source("半山听雨", "F", 70, "score_14_ban_shan_ting_yu.txt"),
                source("空山静", "E", 72, "score_15_kong_shan_jing.txt"),
                source("乌兰巴托的夜", "C", 62, "score_16_wulanbatuo_de_ye.txt"),
                source("葬花吟", "F", 72, "score_17_zang_hua_yin.txt"),
                source("寒山僧踪", "F", 72, "score_18_han_shan_seng_zong.txt"),
                source("月满西楼", "G", 65, "score_19_yue_man_xi_lou.txt"),
                source("烟花易冷（一）", "C#", 78, "score_20_yan_hua_yi_leng_v1.txt"),
                source("假如爱有天意", "F", 72, "score_21_jia_ru_ai_you_tian_yi.txt"),
                source("山鬼", "F", 62, "score_22_shan_gui.txt"),
                source("秋意浓", "C", 68, "score_23_qiu_yi_nong.txt"),
                source("超越时空的思念", "Bb", 80, "score_24_chao_yue_shi_kong_de_si_nian.txt"),
                source("这世界那么多人", "G", 76, "score_25_zhe_shi_jie_na_me_duo_ren.txt"),
                source("相思", "C", 100, "score_26_xiang_si.txt"),
                source("西海情歌", "Bb", 75, "score_27_xi_hai_qing_ge.txt"),
                source("绿野仙踪", "F", 56, "score_28_lv_ye_xian_zong.txt"),
                source("牵丝戏", "F", 86, "score_29_qian_si_xi.txt"),
                source("烟花易冷（二）", "C", 78, "score_30_yan_hua_yi_leng_v2.txt")
        );
    }

    private static JianpuTextSource source(String title, String key, int tempoBpm, String fileName) {
        return new JianpuTextSource(title, key, tempoBpm, 4, BASE + fileName);
    }
}
