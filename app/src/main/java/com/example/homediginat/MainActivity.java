package com.example.homediginat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import com.google.gson.Gson;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import java.util.Collections;
import java.util.Random;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
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
    private static final String PREFS_NAME = "launcher_prefs";
    private static final String CARD_LIST_KEY = "cardList";
    private RelativeLayout rootLayout;
    private TextView storageInfo ;
    private GridView gridView;
    private RecyclerView recyclerView;

    private TextView externalStorageText;

    private TextView batteryLevelTextView;
    private FloatingActionButton rotateButton;
    private FloatingActionButton addDirectory;
    private SharedPreferences prefs;
    private List<CardModel> cardList ;
    private Gson gson = new Gson();
    CardAdapter adapter;

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
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (savedInstanceState != null) {
            // Restore cardList from savedInstanceState
            String json = savedInstanceState.getString(PREFS_NAME, null);
            if (json != null) {
                Type type = new TypeToken<ArrayList<CardModel>>() {}.getType();
                cardList = gson.fromJson(json, type);
            }
        } else {
            // Load the list from SharedPreferences only if there's no saved state
            cardList = loadCardList();
        }



        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);


        rootLayout = findViewById(R.id.root_layout);
        storageInfo = findViewById(R.id.storage);
        addDirectory = findViewById(R.id.addDirectory);
        rotateButton = findViewById(R.id.rotate);
        batteryLevelTextView = findViewById(R.id.batteryLevelText);
        externalStorageText = findViewById(R.id.externalStorageText);
        recyclerView = findViewById(R.id.recyclerView);
        adapter = new CardAdapter(
                this,
                new ArrayList<>(),
                () -> {
                    saveCardList();
                   // Delete callback, do something when a card is deleted
                    // Example: refresh UI or show message
                },
                selectedCard -> {
                    // Item click callback, open folder in Solid Explorer
                    openFolderInSolidExplorer(selectedCard.getCard2File());
                }
        );
        recyclerView.setAdapter(adapter);
        adapter.updateList(cardList);
        setupRecyclerView();

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, 0) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {

                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();

                // Swap items in your data set
                Collections.swap(cardList, fromPos, toPos);

                // Notify adapter of item moved
                adapter.notifyItemMoved(fromPos, toPos);
                saveCardList();
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                // No swipe action
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true; // Enable long press to drag and drop
            }
        });

        // Attach ItemTouchHelper to your RecyclerView
        itemTouchHelper.attachToRecyclerView(recyclerView);


        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);

        IntentFilter filter1 = new IntentFilter();
        filter1.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter1.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter1.addDataScheme("file");

        registerReceiver(storageReceiver, filter1);
//        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                CardModel selectedCard = cardList.get(position);
//                openFolderInSolidExplorer(selectedCard.getCard2File());
//            }
//        });




        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }


        Log.d("DEBUG", "Parent class: " + rootLayout.getParent().getClass().getSimpleName());
        storageInfo.setText(bytesToHuman(freeMemory()) + " unused");

        File documentsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File picturesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        addDirectory.setOnClickListener(v -> {
            selectFolder();  // Call your add card function
        });

        storageInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSolidExplorer();
            }
        });


        //TODO fix rotateButton
        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSolidExplorer();
            }
        });
    }

    private void setupRecyclerView() {
        int orientation = getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Portrait: 2 columns vertical
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            // Landscape: 1 row horizontal
            GridLayoutManager layoutManager = new GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false);
            recyclerView.setLayoutManager(layoutManager);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Save current position before changing layout
        int currentPosition = ((GridLayoutManager)recyclerView.getLayoutManager())
                .findFirstVisibleItemPosition();

        setupRecyclerView();

        // Restore position after layout change
        recyclerView.getLayoutManager().scrollToPosition(currentPosition);
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
                adapter.addCard(newCard);
                saveCardList();
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();

        // Save the scroll position
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
            int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

            // Save the position in SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("scroll_position", firstVisibleItemPosition);
            editor.apply();
        }

        // Save the card list after rearranging
        saveCardList();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save cardList as a JSON string
        String json = gson.toJson(cardList);
        outState.putString(PREFS_NAME, json);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        String json = savedInstanceState.getString(PREFS_NAME, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<CardModel>>() {}.getType();
            List<CardModel> restoredList = gson.fromJson(json, type);
            if (restoredList != null) {
                adapter.updateList(restoredList); // ✅ Update adapter with saved list
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reapply the layout manager based on the current orientation
        setupRecyclerView();

        // Restore the scroll position after reapplying the layout manager
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
            // Restore the scroll position after the layout manager is applied
            int scrollPosition = prefs.getInt("scroll_position", 0);
            layoutManager.scrollToPosition(scrollPosition);
        }
    }
    private String getFolderName(Uri uri) {
        String docId = DocumentsContract.getTreeDocumentId(uri);
        String[] split = docId.split(":");

        String pathPart = split.length >= 2 ? split[1] : split[0];

        // Split the path on "/" to get the last folder name
        String[] pathSegments = pathPart.split("/");
        return pathSegments[pathSegments.length - 1]; // Get the last segment
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
        //TODO
    }

    private void updateLayoutForLandscape() {
        //TODO
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