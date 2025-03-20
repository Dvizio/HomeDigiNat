package com.example.homediginat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import com.google.gson.Gson;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import java.util.Random;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.StrictMode;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_PICK_FOLDER = 42;
    private static final String PREFS_NAME = "myPrefs";
    private static final String CARD_LIST_KEY = "cardList";
    private RelativeLayout rootLayout;
    private TextView storageInfo ;
    private GridView gridView;
    private TextView folderBoxText1;
    private TextView externalStorageText;
    private TextView folderBoxText2;
    private TextView batteryLevelTextView;
    private FloatingActionButton rotateButton;
    private FloatingActionButton addDirectory;
    private FrameLayout folderBox1;
    private FrameLayout folderBox2;
    private SharedPreferences prefs;
    private List<CardModel> cardList ;
    private Gson gson = new Gson();
    CardAdapter adapter;


    private int currentFolderBeingSet = 0;
    private boolean isRotated = false;

    class Pac{
        Drawable icon;
        String name;
        String label;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);



        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE);
        rootLayout = findViewById(R.id.root_layout);
        storageInfo = findViewById(R.id.storage);
        addDirectory = findViewById(R.id.addDirectory);
        rotateButton = findViewById(R.id.rotate);
        folderBox1 = findViewById(R.id.folderBox1);
        folderBox2 = findViewById(R.id.folderBox2);
        folderBoxText1 = findViewById(R.id.folderBoxText1);
        folderBoxText2 = findViewById(R.id.folderBoxText2);
        batteryLevelTextView = findViewById(R.id.batteryLevelText);
        externalStorageText = findViewById(R.id.externalStorageText);
        gridView = findViewById(R.id.gridView);
        cardList = loadCardList();
        adapter = new CardAdapter(this,(ArrayList<CardModel>) cardList);
        gridView.setAdapter(adapter);

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);

        IntentFilter filter1 = new IntentFilter();
        filter1.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter1.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter1.addDataScheme("file");

        registerReceiver(storageReceiver, filter1);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CardModel selectedCard = cardList.get(position);
                openFolderInSolidExplorer(selectedCard.getCard2File());
            }
        });




        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }


        Log.d("DEBUG", "Parent class: " + rootLayout.getParent().getClass().getSimpleName());
        storageInfo.setText(bytesToHuman(freeMemory()) + " unused");

        File documentsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File picturesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        folderBoxText2.setText(picturesFolder.getName());
        folderBoxText1.setText(documentsFolder.getName());
        String defaultDocumentsPath = documentsFolder.getAbsolutePath();
        String defaultPicturesPath = picturesFolder.getAbsolutePath();

        if (!prefs.contains("folder1_path")) {
            defaultDocumentsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
            prefs.edit().putString("folder1_path", defaultDocumentsPath).apply();
        }
        if (!prefs.contains("folder2_path")) {
            defaultPicturesPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
            prefs.edit().putString("folder2_path", defaultPicturesPath).apply();
        }

        addDirectory.setOnClickListener(v -> {
            selectFolder();  // Call your add card function
        });

        //Folder box opener
        folderBox1.setOnClickListener(v -> {
            String folder1Path = prefs.getString("folder1_path", "");
            openFolderInSolidExplorer(folder1Path);
        });
        folderBox2.setOnClickListener(v -> {
            String folder2Path = prefs.getString("folder2_path", "");
            openFolderInSolidExplorer(folder2Path);
        });
        storageInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSolidExplorer();
            }
        });

        folderBox1.setOnLongClickListener(v -> {
            currentFolderBeingSet = 1; // You can track which button was pressed
            selectFolder();
            folderBoxText1.setText(documentsFolder.getName());
            return true;
        });

        folderBox2.setOnLongClickListener(v -> {
            currentFolderBeingSet = 2;
            selectFolder();
            folderBoxText2.setText(picturesFolder.getName());
            return true;
        });



        //TODO fix rotateButton
        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSolidExplorer();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == RESULT_OK) {
            Uri treeUri = data.getData();
            if (treeUri != null) {

                // Persist permissions
                final int takeFlags = data.getFlags() &
                        (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                // Ensure at least one flag is set
                int persistFlags = takeFlags;
                if (persistFlags == 0) {
                    // Fallback: manually specify read/write permissions if none detected
                    persistFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                }

                getContentResolver().takePersistableUriPermission(treeUri, persistFlags);

                // Get folder name for display (optional)
                String folderName = getFolderName(treeUri);

                // ✅ Get the actual file path using your function
                String fullFolderPath = getFullPathFromTreeUri(treeUri);

                if (fullFolderPath == null || fullFolderPath.isEmpty()) {
                    Toast.makeText(this, "Failed to get full path for folder!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // ✅ Log it for debugging
                Log.d("FolderPicker", "Folder Name: " + folderName);
                Log.d("FolderPicker", "Tree URI: " + treeUri.toString());
                Log.d("FolderPicker", "Full Path: " + fullFolderPath);

                // ✅ Create a new CardModel using the full path instead of URI string
                // You can choose whether to store the treeUri.toString() or the full path
                CardModel newCard = new CardModel(folderName, R.drawable.ic_launcher_foreground, fullFolderPath);

                // Add to list and save
                cardList.add(newCard);
                saveCardList();
                adapter.notifyDataSetChanged();
            }
        }
    }
    private String getFolderName(Uri uri) {
        String docId = DocumentsContract.getTreeDocumentId(uri);
        String[] split = docId.split(":");
        return split.length >= 2 ? split[1] : split[0]; // Folder name fallback
    }
    private List<CardModel> loadCardList() {
        String json = prefs.getString(CARD_LIST_KEY, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<CardModel>>() {}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>(); // No data yet
    }
    private void saveCardList() {
        SharedPreferences.Editor editor = prefs.edit();
        String json = gson.toJson(cardList);
        editor.putString(CARD_LIST_KEY, json);
        editor.apply(); // Or commit()
    }

    public void selectFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION); // Optional but helps with subfolder access
        startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER);
    }
    private void openSolidExplorer() {
        String packageName = "pl.solidexplorer2";
        PackageManager packageManager = getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(packageName);

        if (intent != null) {
            startActivity(intent); // Open Solid Explorer
        } else {
            Toast.makeText(this, "Solid Explorer is not installed", Toast.LENGTH_SHORT).show();
        }
    }

    public void openFolderInSolidExplorer(String folderPath) {
        if (folderPath == null || folderPath.isEmpty()) {
            Toast.makeText(this, "No folder set!", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri;

        if (folderPath.startsWith("/")) {
            // It's a file path
            uri = Uri.parse("file://" + folderPath);
        } else if (folderPath.startsWith("content://")) {
            // It's a content Uri
            uri = Uri.parse(folderPath);
        } else {
            Toast.makeText(this, "Invalid folder path!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("SolidExplorerOpen", "Opening URI: " + uri.toString());

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "resource/folder");
        intent.setPackage("pl.solidexplorer2");

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Solid Explorer not found!", Toast.LENGTH_SHORT).show();
        }
    }


    private BroadcastReceiver storageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Uri data = intent.getData();

            if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                Log.d("Storage", "Mounted: " + data.getPath());
                externalStorageText.setText("External Storage Connected");
            } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
                Log.d("Storage", "Unmounted: " + data.getPath());
                externalStorageText.setText("External Storage Disconnected");
            }
        }
    };


    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            if (isCharging) {
                Log.d("BatteryStatus", "Charging: " + isCharging);
            }

            int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
            boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            float batteryPct = level * 100 / (float) scale;

            Log.d("BatteryStatus", "Battery level: " + batteryPct + "%");

            // You can update your UI here if you want!
            batteryLevelTextView.setText("Current Charge: " + batteryPct + "%");
        }
    };

    private void updateLayoutForPortrait() {
        // Update rootLayout (RelativeLayout)
        RelativeLayout.LayoutParams rootParams = (RelativeLayout.LayoutParams) rootLayout.getLayoutParams();
        rootParams.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        rootParams.height = RelativeLayout.LayoutParams.MATCH_PARENT;
        rootLayout.setLayoutParams(rootParams);

        // Update LinearLayout (if it exists)
        LinearLayout linearLayout = findViewById(R.id.linear_layout); // Replace with your LinearLayout ID
        if (linearLayout != null) {
            LinearLayout.LayoutParams linearParams = (LinearLayout.LayoutParams) linearLayout.getLayoutParams();
            linearParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
            linearParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            linearLayout.setLayoutParams(linearParams);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
        }

        // Update FrameLayouts (if they exist)
        FrameLayout frameLayout1 = findViewById(R.id.folderBox1); // Replace with your FrameLayout ID
        if (frameLayout1 != null) {
            FrameLayout.LayoutParams frameParams = (FrameLayout.LayoutParams) frameLayout1.getLayoutParams();
            frameParams.width = FrameLayout.LayoutParams.MATCH_PARENT;
            frameParams.height = 100; // Adjust height as needed
            frameLayout1.setLayoutParams(frameParams);
        }
    }

    private void updateLayoutForLandscape() {
        // Update rootLayout (RelativeLayout)
        RelativeLayout.LayoutParams rootParams = (RelativeLayout.LayoutParams) rootLayout.getLayoutParams();
        rootParams.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        rootParams.height = RelativeLayout.LayoutParams.MATCH_PARENT;
        rootLayout.setLayoutParams(rootParams);

        // Update LinearLayout (if it exists)
        LinearLayout linearLayout = findViewById(R.id.linear_layout); // Replace with your LinearLayout ID
        if (linearLayout != null) {
            LinearLayout.LayoutParams linearParams = (LinearLayout.LayoutParams) linearLayout.getLayoutParams();
            linearParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
            linearParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            linearLayout.setLayoutParams(linearParams);
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        }

        // Update FrameLayouts (if they exist)
        FrameLayout frameLayout1 = findViewById(R.id.folderBox1); // Replace with your FrameLayout ID
        if (frameLayout1 != null) {
            FrameLayout.LayoutParams frameParams = (FrameLayout.LayoutParams) frameLayout1.getLayoutParams();
            frameParams.width = 100; // Adjust width as needed
            frameParams.height = FrameLayout.LayoutParams.MATCH_PARENT;
            frameLayout1.setLayoutParams(frameParams);
        }
    }

    public String getFullPathFromTreeUri(Uri uri) {
        if (uri == null) return null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String volumePath = getVolumePath(getVolumeIdFromTreeUri(uri));
            if (volumePath == null) return null;

            String documentPath = getDocumentPathFromTreeUri(uri);
            if (documentPath != null && documentPath.length() > 0) {
                return volumePath + "/" + documentPath;
            }
            return volumePath;
        }

        return null;
    }

    public String getVolumeIdFromTreeUri(Uri uri) {
        final String docId = DocumentsContract.getTreeDocumentId(uri);
        final String[] split = docId.split(":");
        return split[0];
    }

    public String getDocumentPathFromTreeUri(Uri uri) {
        final String docId = DocumentsContract.getTreeDocumentId(uri);
        final String[] split = docId.split(":");
        return split.length > 1 ? split[1] : "";
    }

    public String getVolumePath(final String volumeId) {
        try {
            StorageManager mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");

            List<StorageVolume> storageVolumes = mStorageManager.getStorageVolumes();
            for (StorageVolume storageVolume : storageVolumes) {
                if (volumeId.equals("primary") && storageVolume.isPrimary()) {
                    return storageVolume.getDirectory().getAbsolutePath();
                }

                String uuid = (String) storageVolumeClazz.getMethod("getUuid").invoke(storageVolume);
                if (uuid != null && uuid.equals(volumeId)) {
                    return storageVolume.getDirectory().getAbsolutePath();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }



    public long freeMemory() {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        long free = (statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong());
        return free;
    }


    public static String floatForm (double d)
    {
        return new DecimalFormat("#.##").format(d);
    }


    public static String bytesToHuman (long size)
    {
        long Kb = 1024;
        long Mb = Kb * 1024;
        long Gb = Mb * 1024;
        long Tb = Gb * 1024;
        long Pb = Tb * 1024;
        long Eb = Pb * 1024;

        if (size <  Kb)                 return floatForm(        size     ) + " byte";
        if (size >= Kb && size < Mb)    return floatForm((double)size / Kb) + " KB";
        if (size >= Mb && size < Gb)    return floatForm((double)size / Mb) + " MB";
        if (size >= Gb && size < Tb)    return floatForm((double)size / Gb) + " GB";
        if (size >= Tb && size < Pb)    return floatForm((double)size / Tb) + " TB";
        if (size >= Pb && size < Eb)    return floatForm((double)size / Pb) + " PB";
        if (size >= Eb)                 return floatForm((double)size / Eb) + " EB";

        return "???";
    }

}