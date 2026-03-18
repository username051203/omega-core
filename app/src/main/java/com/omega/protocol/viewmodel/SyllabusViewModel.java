package com.omega.protocol.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.omega.protocol.db.OmegaRepository;
import com.omega.protocol.model.*;
import java.util.*;
import java.util.concurrent.Executors;

public class SyllabusViewModel extends AndroidViewModel {

    private final OmegaRepository repo;
    private final MutableLiveData<List<Subject>> subjects = new MutableLiveData<>();
    public LiveData<List<Subject>> getSubjects() { return subjects; }

    public SyllabusViewModel(@NonNull Application app) {
        super(app);
        repo = OmegaRepository.get(app);
    }

    public void load() {
        Executors.newSingleThreadExecutor().execute(() ->
            subjects.postValue(repo.getSubjectsSync()));
    }

    public void addSubject(String name, Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Subject s = new Subject("subj_" + System.currentTimeMillis(), name);
            repo.saveSubject(s);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.run(); load();
            });
        });
    }

    public void deleteSubject(String id, Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            repo.deleteSubject(id);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.run(); load();
            });
        });
    }

    public void addChapter(String subjectId, String name, Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Subject> list = repo.getSubjectsSync();
            for (Subject s : list) {
                if (s.id.equals(subjectId)) {
                    Chapter c = new Chapter("ch_" + System.currentTimeMillis(), name);
                    s.chapters.add(c);
                    repo.saveSubject(s);
                    break;
                }
            }
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.run(); load();
            });
        });
    }

    public void addTopic(String subjectId, String chapterId, String name, Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Subject> list = repo.getSubjectsSync();
            for (Subject s : list) {
                if (s.id.equals(subjectId)) {
                    for (Chapter c : s.chapters) {
                        if (c.id.equals(chapterId)) {
                            c.topics.add(new Topic("t_" + System.currentTimeMillis(), name));
                            repo.saveSubject(s);
                            break;
                        }
                    }
                    break;
                }
            }
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.run(); load();
            });
        });
    }

    public void deleteTopic(String subjectId, String chapterId, String topicId, Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Subject> list = repo.getSubjectsSync();
            for (Subject s : list) {
                if (s.id.equals(subjectId)) {
                    for (Chapter c : s.chapters) {
                        if (c.id.equals(chapterId)) {
                            c.topics.removeIf(t -> t.id.equals(topicId));
                            repo.saveSubject(s);
                            break;
                        }
                    }
                    break;
                }
            }
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.run(); load();
            });
        });
    }

    public void batchImport(String subjectId, List<String[]> chapterTopicPairs, Runnable onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Subject> list = repo.getSubjectsSync();
            Subject target = null;
            for (Subject s : list) if (s.id.equals(subjectId)) { target = s; break; }
            if (target == null) return;
            for (String[] pair : chapterTopicPairs) {
                String chName = pair[0].trim(), tName = pair[1].trim();
                Chapter found = null;
                for (Chapter c : target.chapters) if (c.name.equals(chName)) { found = c; break; }
                if (found == null) { found = new Chapter("ch_"+System.currentTimeMillis()+"_"+chName.hashCode(), chName); target.chapters.add(found); }
                found.topics.add(new Topic("t_"+System.currentTimeMillis()+"_"+tName.hashCode(), tName));
            }
            repo.saveSubject(target);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.run(); load();
            });
        });
    }
}
