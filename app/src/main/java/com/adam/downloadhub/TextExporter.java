package com.adam.downloadhub;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public final class TextExporter {
    private TextExporter() {}

    public static StreamingSession openStreaming(Context context) throws Exception {
        return new StreamingSession(context);
    }

    public static final class StreamingSession implements AutoCloseable {
        private final ContentResolver resolver;

        private Uri channelsUri;
        private Uri moviesUri;
        private Uri seriesUri;

        private BufferedWriter channelsWriter;
        private BufferedWriter moviesWriter;
        private BufferedWriter seriesWriter;

        private long channelsCount;
        private long moviesCount;
        private long seriesCount;
        private boolean finished;
        private StreamingExportResult result;

        private StreamingSession(Context context) throws Exception {
            resolver = context.getContentResolver();
            try {
                Target channels = createTarget("Channels.txt", "القنوات");
                channelsUri = channels.uri;
                channelsWriter = channels.writer;

                Target movies = createTarget("Movies.txt", "الأفلام");
                moviesUri = movies.uri;
                moviesWriter = movies.writer;

                Target series = createTarget("Series.txt", "المسلسلات");
                seriesUri = series.uri;
                seriesWriter = series.writer;
            } catch (Exception e) {
                cleanup();
                throw e;
            }
        }

        public void write(M3uParser.Entry entry, M3uParser.Type type) throws Exception {
            if (finished) throw new IllegalStateException("تم إنهاء التصدير بالفعل");
            if (type == M3uParser.Type.SERIES) {
                seriesCount++;
                writeEntry(seriesWriter, seriesCount, entry);
            } else if (type == M3uParser.Type.MOVIE) {
                moviesCount++;
                writeEntry(moviesWriter, moviesCount, entry);
            } else {
                channelsCount++;
                writeEntry(channelsWriter, channelsCount, entry);
            }
        }

        public StreamingExportResult finish() throws Exception {
            if (finished) return result;
            try {
                appendTotal(channelsWriter, channelsCount);
                appendTotal(moviesWriter, moviesCount);
                appendTotal(seriesWriter, seriesCount);

                closeWriter(channelsWriter);
                channelsWriter = null;
                closeWriter(moviesWriter);
                moviesWriter = null;
                closeWriter(seriesWriter);
                seriesWriter = null;

                markComplete(channelsUri);
                markComplete(moviesUri);
                markComplete(seriesUri);

                result = new StreamingExportResult(
                        channelsUri, moviesUri, seriesUri,
                        channelsCount, moviesCount, seriesCount);
                finished = true;
                return result;
            } catch (Exception e) {
                cleanup();
                throw e;
            }
        }

        private Target createTarget(String fileName, String title) throws Exception {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/DownloadHub");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);

            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IllegalStateException("تعذر إنشاء " + fileName);

            try {
                OutputStream out = resolver.openOutputStream(uri, "w");
                if (out == null) throw new IllegalStateException("تعذر فتح " + fileName);
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(out, StandardCharsets.UTF_8), 64 * 1024);
                writer.write("Download Hub - " + title);
                writer.newLine();
                writer.write("يتم إنشاء هذا الملف بطريقة Streaming لدعم قوائم M3U الضخمة.");
                writer.newLine();
                writer.newLine();
                return new Target(uri, writer);
            } catch (Exception e) {
                resolver.delete(uri, null, null);
                throw e;
            }
        }

        private void writeEntry(BufferedWriter writer, long index, M3uParser.Entry e) throws Exception {
            writer.write("[");
            writer.write(Long.toString(index));
            writer.write("]");
            writer.newLine();
            writer.write("الاسم: ");
            writer.write(e.name);
            writer.newLine();
            if (!e.group.isEmpty()) {
                writer.write("المجموعة: ");
                writer.write(e.group);
                writer.newLine();
            }
            writer.write("الرابط: ");
            writer.write(e.url);
            writer.newLine();
            writer.write("EXTINF: ");
            writer.write(e.extInf);
            writer.newLine();
            writer.newLine();
        }

        private void appendTotal(BufferedWriter writer, long count) throws Exception {
            writer.newLine();
            writer.write("العدد الإجمالي: ");
            writer.write(Long.toString(count));
            writer.newLine();
            writer.flush();
        }

        private void markComplete(Uri uri) {
            if (uri == null) return;
            ContentValues done = new ContentValues();
            done.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(uri, done, null, null);
        }

        private void closeWriter(BufferedWriter writer) throws Exception {
            if (writer != null) writer.close();
        }

        private void cleanup() {
            closeQuietly(channelsWriter);
            closeQuietly(moviesWriter);
            closeQuietly(seriesWriter);
            channelsWriter = null;
            moviesWriter = null;
            seriesWriter = null;
            deleteQuietly(channelsUri);
            deleteQuietly(moviesUri);
            deleteQuietly(seriesUri);
        }

        private void closeQuietly(BufferedWriter writer) {
            if (writer == null) return;
            try { writer.close(); } catch (Exception ignored) {}
        }

        private void deleteQuietly(Uri uri) {
            if (uri == null) return;
            try { resolver.delete(uri, null, null); } catch (Exception ignored) {}
        }

        @Override
        public void close() {
            if (!finished) cleanup();
        }
    }

    private static final class Target {
        final Uri uri;
        final BufferedWriter writer;

        Target(Uri uri, BufferedWriter writer) {
            this.uri = uri;
            this.writer = writer;
        }
    }

    public static final class StreamingExportResult {
        public final Uri channels;
        public final Uri movies;
        public final Uri series;
        public final long channelsCount;
        public final long moviesCount;
        public final long seriesCount;

        public StreamingExportResult(
                Uri channels, Uri movies, Uri series,
                long channelsCount, long moviesCount, long seriesCount) {
            this.channels = channels;
            this.movies = movies;
            this.series = series;
            this.channelsCount = channelsCount;
            this.moviesCount = moviesCount;
            this.seriesCount = seriesCount;
        }
    }
}
