package com.dongxiao.practice.song;

public final class OnlineSongResource {
    public final String title;
    public final String source;
    public final String format;
    public final String license;
    public final String url;
    public final boolean importable;

    public OnlineSongResource(
            String title,
            String source,
            String format,
            String license,
            String url,
            boolean importable
    ) {
        this.title = title;
        this.source = source;
        this.format = format;
        this.license = license;
        this.url = url;
        this.importable = importable;
    }

    public String detailText() {
        String status = importable ? "可直接添加到本地" : "当前版本暂不能直接导入";
        return source + " · " + format + " · " + license + "\n" + status;
    }

    @Override
    public String toString() {
        return title;
    }
}
