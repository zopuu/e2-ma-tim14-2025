package com.example.habibitar.ui.bossFight;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habibitar.R;
import com.example.habibitar.data.BossFightRepository.BossFightRepository;
import com.example.habibitar.data.user.UserRepository;
import com.example.habibitar.domain.model.BossFight;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.concurrent.CompletableFuture;

public class BossFightActivity extends AppCompatActivity implements SensorEventListener {

    // --- Statičke liste opreme (u Activity-ju) ---
    private static final String[] CLOTHING = new String[]{
            "Štit od hrasta", "Kaciga legionara", "Pancir MK-I", "Kožni oklop",
            "Čelične naramenice", "Magični plašt", "Rukavice izdržljivosti",
            "Čizme brzine", "Amulet zaštite", "Pojas snage"
    };
    private static final String[] WEAPONS = new String[]{
            "Mač Aegis", "Bodež noći", "Dvoručni mač", "Srebrna sekira",
            "Strele preciznosti", "Taktička puška", "Luk lovca"
    };
    // Shake-to-attack
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private MaterialButton btnActivateEquipment; // toggle
    private TextView tvMyItemsTitle;             // naslov liste
    private ScrollView svMyItems;                // kontejner liste
    private boolean equipmentVisible = false;    // lokalni toggle (ne persisitira se)

    private static final float SHAKE_THRESHOLD_GRAVITY = 2.7f; // podešavaj između ~2.3–3.0
    private static final int SHAKE_SLOP_TIME_MS = 800;         // debounce
    private long lastShakeTs = 0;

    // UI
    private MaterialToolbar toolbar;
    private LinearProgressIndicator hpBar;
    private TextView tvBossHpText;
    private TextView tvAttacksLeft;
    private TextView tvUserPP;
    private MaterialButton btnAttack;
    private ImageView ivBoss;
    // UI
    private TextView tvRewardsList; // <-- NOVO

    // Auth / repo
    private final UserRepository userRepo = new UserRepository();
    private final BossFightRepository fightRepo = new BossFightRepository();
    private FirebaseAuth auth;
    private String uid;

    // Fight state
    private String fightId;                // aktivni fight doc id
    private int bossLevel = 1;             // nivo trenutnog bossa
    private int userPP = 0;                // PP korisnika (iz Firestore)
    private int initialBossHp;             // max HP (za bar)
    private int currentBossHp;             // trenutno stanje HP
    private int attacksLeft;               // 5 pokušaja po borbi
    private int currentHitChancePercent;   // 50..100 po pokušaju
    private int moneyReward;               // nagrada (trenutna logika: 200 * 1.2^(level-1))
    private TextView tvMyItemsList; // 👈 novo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.boss_fight);

        // Bind UI
        toolbar = findViewById(R.id.toolbar);
        ivBoss = findViewById(R.id.ivBoss);
        hpBar = findViewById(R.id.hpBar);
        tvBossHpText = findViewById(R.id.tvBossHpText);
        tvAttacksLeft = findViewById(R.id.tvAttacksLeft);
        tvUserPP = findViewById(R.id.tvUserPP);
        btnAttack = findViewById(R.id.btnAttack);
        tvRewardsList = findViewById(R.id.tvRewardsList);
        tvMyItemsList = findViewById(R.id.tvMyItemsList);

        btnActivateEquipment = findViewById(R.id.btnActivateEquipment);
        tvMyItemsTitle       = findViewById(R.id.tvMyItemsTitle);
        svMyItems            = findViewById(R.id.svMyItems);

        tvMyItemsTitle.setVisibility(View.GONE);
        svMyItems.setVisibility(View.GONE);
        btnActivateEquipment.setText("Activate equipment");

        btnActivateEquipment.setOnClickListener(v -> {
            equipmentVisible = !equipmentVisible;
            tvMyItemsTitle.setVisibility(equipmentVisible ? View.VISIBLE : View.GONE);
            svMyItems.setVisibility(equipmentVisible ? View.VISIBLE : View.GONE);
            btnActivateEquipment.setText(
                    equipmentVisible ? "Hide equipment" : "Activate equipment"
            );
        });

        populateRewardsUi(); // prikaži moguće dropove

        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        // Default ulazi (ako profil još nije učitan)
        bossLevel = getIntent().getIntExtra("level", 1);
        attacksLeft = 5;
        currentHitChancePercent = randomHitChance();
        recomputeFromLevel(); // postavi HP i reward za trenutni nivo

        // UI init
        hpBar.setMax(initialBossHp);
        hpBar.setProgress(currentBossHp);
        updateUi();
        setToolbarSubtitle();

        // Auth + profil
        auth = FirebaseAuth.getInstance();
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser == null) {
            Toast.makeText(this, "Niste prijavljeni.", Toast.LENGTH_SHORT).show();
            btnAttack.setEnabled(false);
            return;
        }
        uid = fbUser.getUid();

        // 1) Učitaj PP i (po želji) bossLevel iz profila
        userRepo.getProfile(uid)
                .thenAccept(this::handleProfileLoaded)
                .exceptionally(ex -> { runOnUiThread(() ->
                        Toast.makeText(this, "Greška profila: " + ex.getMessage(), Toast.LENGTH_SHORT).show());
                    return null; });

        // 2) Ako postoji nezavršena borba, nastavi je, pa odmah “self-heal” ako je korumpirana
        fightRepo.getActiveForUser(uid).thenAccept(active -> {
            if (active != null) {
                fightId = active.getId();
                bossLevel = active.getLevel();
                currentBossHp = active.getBossXp(); // getter vraća bossHp
                attacksLeft = active.getUsersAttacksLeft();
                moneyReward = active.getMoneyReward();
                initialBossHp = computeBossHpForLevel20(bossLevel);
                runOnUiThread(() -> {
                    setToolbarSubtitle();
                    hpBar.setMax(initialBossHp);
                    updateUi();
                    ensureActiveFightIsValid(); // 👈 odmah popravi ako je HP≤0
                });
            }
        });

        // Napad
        btnAttack.setOnClickListener(v -> handleAttack());

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

    }
    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null && sensorManager != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // normalizuj na gravitaciju zemlje
        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            long now = SystemClock.elapsedRealtime();
            if (now - lastShakeTs < SHAKE_SLOP_TIME_MS) return; // debounce
            lastShakeTs = now;

            // opcionalno: ne pokušavaj napad ako je dugme onemogućeno
            if (btnAttack == null || !btnAttack.isEnabled()) return;

            // isto kao klik na "Napadni"
            handleAttack();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // nije nam potrebno
    }

    private void handleProfileLoaded(DocumentSnapshot snap) {
        int loadedPP = 0;
        int loadedBossLevel = bossLevel;
        if (snap != null && snap.exists()) {
            Long ppVal = snap.getLong("pp");
            if (ppVal != null) loadedPP = ppVal.intValue();

            Long bl = snap.getLong("bossLevel");
            if (bl != null && bl > 0) loadedBossLevel = bl.intValue();
        }
        final int finalPP = loadedPP;
        final int finalBossLevel = loadedBossLevel;
        java.util.List<String> eq = null;
        if (snap != null && snap.exists()) {
            Object raw = snap.get("equipment");
            if (raw instanceof java.util.List) {
                //noinspection unchecked
                eq = (java.util.List<String>) raw;
            }
        }
        final java.util.List<String> equipment = eq;
        runOnUiThread(() -> {
            userPP = finalPP;
            if (tvUserPP != null) tvUserPP.setText("PP: " + userPP);
            renderMyEquipment(equipment);
            if (fightId == null) {
                bossLevel = finalBossLevel;
                recomputeFromLevel();
                setToolbarSubtitle();
                hpBar.setMax(initialBossHp);
                updateUi();
            }
        });
    }
    private void renderMyEquipment(java.util.List<String> items) {
        if (tvMyItemsList == null) return;
        if (items == null || items.isEmpty()) {
            tvMyItemsList.setText("— još nemaš opremu —");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : items) sb.append("• ").append(s).append('\n');
        tvMyItemsList.setText(sb.toString());
    }

    /** Klik na "Napadni". */
    private void handleAttack() {
        // Ako je fight već u čudnom stanju (HP 0), prvo se oporavi/napreduj.
        if (currentBossHp <= 0) {
            ensureActiveFightIsValid();
            return;
        }
        if (attacksLeft <= 0) {
            showResult();
            return;
        }

        // Ako fight ne postoji u bazi — kreiramo ga, pa onda udarimo
        if (fightId == null) {
            btnAttack.setEnabled(false);
            BossFight f = new BossFight(
                    null,
                    uid,
                    bossLevel,
                    initialBossHp,  // bossHp na startu
                    attacksLeft,
                    moneyReward,
                    null,
                    false
            );
            fightRepo.create(uid, f)
                    .thenAccept(created -> {
                        fightId = created.getId();
                        runOnUiThread(() -> {
                            btnAttack.setEnabled(true);
                            performAttackAndPersist();
                        });
                    })
                    .exceptionally(ex -> { runOnUiThread(() -> {
                        btnAttack.setEnabled(true);
                        Toast.makeText(this, "Ne mogu da započnem borbu: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                        return null; });
            return;
        }

        performAttackAndPersist();
    }

    /** Izvedi pokušaj napada, sačuvaj stanje i proveri kraj. */
    private void performAttackAndPersist() {
        int roll = (int) (Math.random() * 100); // 0..99
        boolean hit = roll < currentHitChancePercent;

        attacksLeft--; // troši pokušaj

        if (hit) {
            int dmg = Math.max(0, userPP); // skini tačno PP sa HP bossa
            currentBossHp = Math.max(0, currentBossHp - dmg);
            Toast.makeText(this, "Pogodak! -" + dmg + " HP", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Promašaj!", Toast.LENGTH_SHORT).show();
        }

        if (attacksLeft > 0 && currentBossHp > 0) {
            currentHitChancePercent = randomHitChance(); // nova šansa za sledeći pokušaj
        }

        updateUi();

        // persist trenutnog stanja
        if (fightId != null) {
            fightRepo.updateState(fightId, currentBossHp, attacksLeft);
        }

        // kraj borbe?
        if (attacksLeft == 0 || currentBossHp == 0) {
            if (currentBossHp == 0) {
                // POBEDA: završi borbu, nagradi i pređi na NOVOG bossa
                btnAttack.setEnabled(false);
                fightRepo.finish(fightId);

                userRepo.incrementCoins(uid, moneyReward)
                        .thenCompose(v -> userRepo.incrementBossLevel(uid, 1))
                        .thenCompose(v -> maybeGrantEquipment()) // 👈 60% drop; 95% odeća/5% oružje
                        .thenCompose(wonItemName -> {
                            if (wonItemName != null) {
                                Snackbar.make(findViewById(R.id.root),
                                        "Dobio si opremu: " + wonItemName + " 🎁",
                                        Snackbar.LENGTH_LONG).show();
                            }
                            return createNextBossAndResetUi(); // kreiraj novi BossFight + reset UI
                        })
                        .exceptionally(ex -> { runOnUiThread(() -> {
                            Toast.makeText(this, "Greška prelaska na novog bossa: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                            btnAttack.setEnabled(true);
                        });
                            return null; });
            }

            showResult(); // kratki toast o ishodu
        }
    }

    /** 60% šansa da dodelimo opremu; 95% odeća / 5% oružje. Vraća ime osvojenog itema ili null. */
    private CompletableFuture<String> maybeGrantEquipment() {
        CompletableFuture<String> fut = new CompletableFuture<>();
        int roll = (int) (Math.random() * 100); // 0..99
        if (roll >= 60) { // nema dropa
            fut.complete(null);
            return fut;
        }
        String item = randomDropItem95to5();
        userRepo.appendEquipmentItem(uid, item)
                .thenAccept(v -> {
                    fut.complete(item);
                    runOnUiThread(() -> {
                        // Optimistički dodaj liniju dole; (ili ponovo učitaj profil ako želiš čistu istinu)
                        if (tvMyItemsList != null) {
                            CharSequence prev = tvMyItemsList.getText();
                            String line = (prev == null || prev.toString().startsWith("—"))
                                    ? "• " + item
                                    : prev + "\n• " + item;
                            tvMyItemsList.setText(line);
                        }
                    });
                });

        return fut;
    }

    // --- Anti-korupcija aktivnog fighta (HP=0 bez napada) ---
    private void ensureActiveFightIsValid() {
        if (fightId == null) return;

        if (currentBossHp <= 0) {
            // Case A: novi fight je "pokvaren" (HP=0, a niko ga nije napao)
            if (attacksLeft == 5) {
                fightRepo.finish(fightId);         // zatvori loš dokument
                recreateCurrentBossSameLevel();    // re-kreiraj isti nivo bez nagrade
            } else {
                // Case B: fight je faktički završen; zatvori i pređi na sledećeg bez nagrade
                fightRepo.finish(fightId);
                advanceToNextBossWithoutAward();
            }
        }
    }

    private void recreateCurrentBossSameLevel() {
        btnAttack.setEnabled(false);
        initialBossHp = computeBossHpForLevel20(bossLevel);
        currentBossHp = initialBossHp;
        attacksLeft = 5;
        moneyReward = computeRewardForLevel20(bossLevel);
        currentHitChancePercent = randomHitChance();

        BossFight next = new BossFight(
                null, uid, bossLevel, initialBossHp, attacksLeft, moneyReward, null, false
        );
        fightRepo.create(uid, next)
                .thenAccept(created -> runOnUiThread(() -> {
                    fightId = created.getId();
                    setToolbarSubtitle();
                    hpBar.setMax(initialBossHp);
                    updateUi();
                    btnAttack.setEnabled(true);
                    Toast.makeText(this, "Obnovljen boss (Lvl " + bossLevel + ")", Toast.LENGTH_SHORT).show();
                }))
                .exceptionally(ex -> {
                    runOnUiThread(() -> {
                        btnAttack.setEnabled(true);
                        Toast.makeText(this, "Greška pri obnovi bossa: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    });
                    return null;
                });
    }

    private void advanceToNextBossWithoutAward() {
        btnAttack.setEnabled(false);
        bossLevel += 1;
        initialBossHp = computeBossHpForLevel20(bossLevel);
        currentBossHp = initialBossHp;
        attacksLeft = 5;
        moneyReward = computeRewardForLevel20(bossLevel);
        currentHitChancePercent = randomHitChance();

        BossFight next = new BossFight(
                null, uid, bossLevel, initialBossHp, attacksLeft, moneyReward, null, false
        );
        fightRepo.create(uid, next)
                .thenAccept(created -> runOnUiThread(() -> {
                    fightId = created.getId();
                    setToolbarSubtitle();
                    hpBar.setMax(initialBossHp);
                    updateUi();
                    btnAttack.setEnabled(true);
                    Toast.makeText(this, "Prelazak na Lvl " + bossLevel, Toast.LENGTH_SHORT).show();
                }))
                .exceptionally(ex -> {
                    runOnUiThread(() -> {
                        btnAttack.setEnabled(true);
                        Toast.makeText(this, "Greška prelaska: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    });
                    return null;
                });
    }

    /** Posle pobede: kreira novog bossa (level+1), resetuje UI i vraća future. */
    private CompletableFuture<Void> createNextBossAndResetUi() {
        bossLevel += 1;
        initialBossHp = computeBossHpForLevel20(bossLevel);          // 150% više (2.5x kumulativno)
        currentBossHp = initialBossHp;
        attacksLeft = 5;
        moneyReward = computeRewardForLevel20(bossLevel);            // coin logika kakvu već koristiš
        currentHitChancePercent = randomHitChance();

        BossFight next = new BossFight(
                null, uid, bossLevel, initialBossHp, attacksLeft, moneyReward, null, false
        );

        CompletableFuture<Void> fut = new CompletableFuture<>();
        fightRepo.create(uid, next)
                .thenAccept(created -> {
                    fightId = created.getId();
                    runOnUiThread(() -> {
                        setToolbarSubtitle();
                        hpBar.setMax(initialBossHp);
                        updateUi();
                        Toast.makeText(this, "Novi boss! Lvl " + bossLevel, Toast.LENGTH_SHORT).show();
                        btnAttack.setEnabled(true);
                    });
                    fut.complete(null);
                })
                .exceptionally(ex -> { fut.completeExceptionally(ex); return null; });

        return fut;
    }

    /** UI refresh. */
    private void updateUi() {
        hpBar.setProgress(currentBossHp);
        tvBossHpText.setText("HP: " + currentBossHp + "/" + initialBossHp);
        tvAttacksLeft.setText("Preostali napadi: " + attacksLeft + "/5");
        if (tvUserPP != null) tvUserPP.setText("PP: " + userPP);
    }

    /** Kratka obaveštenja. */
    private void showResult() {
        if (currentBossHp <= 0) {
            Toast.makeText(this, "Pobeda! +" + moneyReward + " 💰", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Borba završena. Bos je preživeo.", Toast.LENGTH_LONG).show();
        }
    }

    private void setToolbarSubtitle() {
        if (toolbar != null) toolbar.setSubtitle("Lvl " + bossLevel);
    }

    /** Izračunaj HP i nagradu za trenutni level i napuni state. */
    private void recomputeFromLevel() {
        initialBossHp = computeBossHpForLevel20(bossLevel);
        currentBossHp = initialBossHp;
        attacksLeft = 5;
        moneyReward = computeRewardForLevel20(bossLevel);
    }

    /** HP(level): prvi 200; svaki sledeći 150% više (2.5x kumulativno). */
    private int computeBossHpForLevel20(int level) {
        if (level <= 0) return 0;
        double hp = 200.0 * Math.pow(2.5, (level - 1));
        return (int) Math.round(hp);
    }

    /** Reward(level): primer 200 * 1.2^(level-1) (po tvojoj aktuelnoj logici). */
    private int computeRewardForLevel20(int level) {
        if (level <= 0) return 0;
        double r = 200.0 * Math.pow(1.2, (level - 1));
        return (int) Math.round(r);
    }

    /** Random šansa [50, 100] za svaki pokušaj. */
    private int randomHitChance() {
        return 50 + (int) (Math.random() * 51);
    }

    // --- Helpers za izbor itema iz lokalnih nizova ---
    private String randomFrom(String[] arr) {
        if (arr == null || arr.length == 0) return null;
        int i = (int) (Math.random() * arr.length);
        return arr[i];
    }

    /** 95% odeća, 5% oružje. */
    private String randomDropItem95to5() {
        int roll = (int) (Math.random() * 100); // 0..99
        return (roll < 95) ? randomFrom(CLOTHING) : randomFrom(WEAPONS);
    }

    private void populateRewardsUi() {
        if (tvRewardsList == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("Šansa za drop: 60%\n");
        sb.append("• Oprema (95%):\n");
        for (String s : CLOTHING) sb.append("   – ").append(s).append('\n');
        sb.append("• Oružje (5%):\n");
        for (String s : WEAPONS) sb.append("   – ").append(s).append('\n');

        tvRewardsList.setText(sb.toString());
    }

}
