package com.dongxiao.practice.song;

import java.util.Arrays;
import java.util.List;

public final class ImageScoreRepository {
    private static final String BASE = "score_images/";

    private ImageScoreRepository() {
    }

    public static List<ImageScore> defaults() {
        return Arrays.asList(
                score("G弦上的咏叹调（小图）", "score_01_g_string_aria_square.jpeg"),
                score("G弦上的咏叹调", "score_02_g_string_aria.jpeg"),
                score("红颜旧", "score_03_hong_yan_jiu.jpeg"),
                score("你离开了南京，从此没有人和我说话", "score_04_nanjing.webp"),
                score("一步之遥", "score_05_por_una_cabeza.jpeg"),
                score("断桥残雪", "score_06_duan_qiao_can_xue.jpeg"),
                score("天宫舞曲", "score_07_tian_gong_wu_qu.jpeg"),
                new ImageScore(
                        "琵琶行",
                        BASE + "score_08_pipa_xing_p1.jpeg",
                        BASE + "score_08_pipa_xing_p2.jpeg"
                ),
                score("莫失莫忘", "score_09_mo_shi_mo_wang.jpeg"),
                score("长相思", "score_10_chang_xiang_si.jpeg"),
                score("倩女幽魂", "score_11_qian_nv_you_hun.jpeg"),
                score("卷珠帘", "score_12_juan_zhu_lian.jpeg"),
                score("入殓师", "score_13_ru_lian_shi.jpeg"),
                score("半山听雨", "score_14_ban_shan_ting_yu.jpeg"),
                score("空山静", "score_15_kong_shan_jing.jpeg"),
                score("乌兰巴托的夜", "score_16_wulanbatuo_de_ye.jpeg"),
                score("葬花吟", "score_17_zang_hua_yin.jpeg"),
                score("寒山僧踪", "score_18_han_shan_seng_zong.jpeg"),
                score("月满西楼", "score_19_yue_man_xi_lou.jpeg"),
                score("烟花易冷（一）", "score_20_yan_hua_yi_leng_v1.jpeg"),
                score("假如爱有天意", "score_21_jia_ru_ai_you_tian_yi.jpeg"),
                score("山鬼", "score_22_shan_gui.jpeg"),
                score("秋意浓", "score_23_qiu_yi_nong.jpeg"),
                score("超越时空的思念", "score_24_chao_yue_shi_kong_de_si_nian.jpeg"),
                score("这世界那么多人", "score_25_zhe_shi_jie_na_me_duo_ren.jpeg"),
                score("相思", "score_26_xiang_si.jpeg"),
                score("西海情歌", "score_27_xi_hai_qing_ge.jpeg"),
                score("绿野仙踪", "score_28_lv_ye_xian_zong.jpeg"),
                score("牵丝戏", "score_29_qian_si_xi.jpeg"),
                score("烟花易冷（二）", "score_30_yan_hua_yi_leng_v2.jpeg")
        );
    }

    private static ImageScore score(String title, String fileName) {
        return new ImageScore(title, BASE + fileName);
    }
}
