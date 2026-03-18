package com.omega.protocol.util;

import com.omega.protocol.model.*;
import java.util.*;

public class MicroSyllabus {
    public static final String SUBJECT_ID = "Microbiology";

    public static Subject build() {
        Subject s = new Subject(SUBJECT_ID, "Microbiology");
        s.chapters.add(ch("mc1","General Microbiology",
            t("mt1","Scientists & Stains"),t("mt2","Microscopes"),t("mt3","Bacterial Anatomy"),
            t("mt4","Bacterial Shapes & Physiology"),t("mt5","Bacterial Genetics"),
            t("mt6","Culture Media"),t("mt7","Sterilization & Disinfection")));
        s.chapters.add(ch("mc2","Gram Positive Bacteria",
            t("mt8","Staphylococcus"),t("mt9","Streptococcus & Enterococci"),
            t("mt11","Corynebacterium, Nocardia, Actinomyces, Listeria"),
            t("mt12","Bacillus & Clostridium"),t("mt13","Mycobacteria")));
        s.chapters.add(ch("mc3","Gram Negative Bacteria",
            t("mt10","Neisseria & Moraxella"),t("mt14","GNB – Part 1"),
            t("mt15","GNB – Part 2"),t("mt16","Vibrio, Pseudomonas, Burkholderia"),
            t("mt17","HBB"),t("mt18","Spirochetes")));
        s.chapters.add(ch("mc4","Atypical Bacteria",
            t("mt19","Chlamydia"),t("mt20","Rickettsiae"),t("mt21","Miscellaneous")));
        s.chapters.add(ch("mc5","Virology",
            t("mt22","Virus – General"),t("mt23","DNA Virus Pt 1"),t("mt24","DNA Virus Pt 2"),
            t("mt25","DNA Virus Pt 3"),t("mt26","RNA Virus Pt 1"),t("mt27","RNA Virus Pt 2"),
            t("mt28","RNA Virus Pt 3"),t("mt29","Prions & Unconventional")));
        s.chapters.add(ch("mc6","Mycology",
            t("mt30","Fungi – General"),t("mt31","Superficial Mycoses"),
            t("mt32","Systemic Mycoses"),t("mt33","Opportunistic Mycoses")));
        s.chapters.add(ch("mc7","Parasitology",
            t("mt34","Protozoa – General"),t("mt35","Blood & Tissue Protozoa"),
            t("mt36","Intestinal Protozoa"),t("mt37","Helminths Pt 1"),
            t("mt38","Helminths Pt 2"),t("mt39","Arthropods & Vectors")));
        s.chapters.add(ch("mc8","Clinical Microbiology",
            t("mt40","Lab Diagnostics"),t("mt41","Antimicrobials Pt 1"),
            t("mt42","Antimicrobials Pt 2"),t("mt43","Antimicrobials Pt 3"),
            t("mt44","Immunology – Basics"),t("mt45","Vaccines & Immunization")));
        return s;
    }

    private static Chapter ch(String id, String name, Topic... topics) {
        Chapter c = new Chapter(id, name);
        Collections.addAll(c.topics, topics);
        return c;
    }
    private static Topic t(String id, String name) { return new Topic(id, name); }
}
