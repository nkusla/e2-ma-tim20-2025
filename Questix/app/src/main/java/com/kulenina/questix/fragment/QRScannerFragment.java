package com.kulenina.questix.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.kulenina.questix.R;
import com.kulenina.questix.databinding.FragmentQrScannerBinding;
import com.kulenina.questix.service.FriendshipService;
import com.kulenina.questix.service.AuthService;
import com.kulenina.questix.utils.QRCodeHelper;
import com.kulenina.questix.viewmodel.QRScannerViewModel;

import java.util.Arrays;
import java.util.List;

public class QRScannerFragment extends Fragment {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;

    private FragmentQrScannerBinding binding;
    private QRScannerViewModel viewModel;
    private CompoundBarcodeView barcodeView;
    private FriendshipService friendshipService;
    private AuthService authService;
    private boolean isFlashOn = false;
    private boolean isScanning = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_qr_scanner, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new QRScannerViewModel();
        binding.setViewModel(viewModel);

        friendshipService = new FriendshipService();
        authService = new AuthService();

        setupBarcodeView();
        setupControls();

        if (checkCameraPermission()) {
            startScanning();
        } else {
            requestCameraPermission();
        }
    }

    private void setupBarcodeView() {
        barcodeView = new CompoundBarcodeView(requireContext());
        barcodeView.setDecoderFactory(new DefaultDecoderFactory(Arrays.asList(com.google.zxing.BarcodeFormat.QR_CODE)));
        barcodeView.setStatusText("");

        binding.cameraPreviewContainer.addView(barcodeView);

        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (!isScanning) {
                    return;
                }

                String qrData = result.getText();
                if (QRCodeHelper.isValidQuestixUserQR(qrData)) {
                    String userId = QRCodeHelper.extractUserIdFromQR(qrData);
                    if (userId != null) {
                        handleQRCodeScanned(userId);
                    }
                } else {
                    viewModel.setStatusMessage("Invalid QR code. Please scan a Questix user QR code.");
                    // Allow scanning to continue for invalid codes
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
                // Optional: Handle result points for visual feedback
            }
        });
    }

    private void setupControls() {
        binding.buttonToggleFlash.setOnClickListener(v -> toggleFlash());
        binding.buttonClose.setOnClickListener(v -> closeScanner());
    }

    private void handleQRCodeScanned(String scannedUserId) {
        isScanning = false; // Stop scanning while processing

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Check if trying to add self
        if (currentUserId.equals(scannedUserId)) {
            viewModel.setStatusMessage("You cannot add yourself as a friend!");
            resumeScanning();
            return;
        }

        viewModel.setIsLoading(true);
        viewModel.setLoadingMessage("Checking user...");

        // First check if user exists
        authService.getUser(scannedUserId)
            .addOnSuccessListener(user -> {
                if (user == null) {
                    viewModel.setIsLoading(false);
                    viewModel.setStatusMessage("User not found!");
                    resumeScanning();
                    return;
                }

                // Check if already friends
                friendshipService.areFriends(currentUserId, scannedUserId)
                    .addOnSuccessListener(areFriends -> {
                        if (areFriends) {
                            viewModel.setIsLoading(false);
                            viewModel.setStatusMessage("You are already friends with " + user.username + "!");
                            resumeScanning();
                        } else {
                            // Add as friend
                            addFriend(scannedUserId, user.username);
                        }
                    })
                    .addOnFailureListener(e -> {
                        viewModel.setIsLoading(false);
                        viewModel.setStatusMessage("Error checking friendship status: " + e.getMessage());
                        resumeScanning();
                    });
            })
            .addOnFailureListener(e -> {
                viewModel.setIsLoading(false);
                viewModel.setStatusMessage("Error finding user: " + e.getMessage());
                resumeScanning();
            });
    }

    private void addFriend(String friendId, String friendUsername) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        viewModel.setLoadingMessage("Adding friend...");

        friendshipService.addFriend(currentUserId, friendId)
            .addOnSuccessListener(aVoid -> {
                viewModel.setIsLoading(false);
                Toast.makeText(getContext(), "Added " + friendUsername + " as friend!", Toast.LENGTH_LONG).show();
                closeScanner(); // Close after successful friend addition
            })
            .addOnFailureListener(e -> {
                viewModel.setIsLoading(false);
                viewModel.setStatusMessage("Error adding friend: " + e.getMessage());
                resumeScanning();
            });
    }

    private void resumeScanning() {
        // Resume scanning after a delay
        if (getView() != null) {
            getView().postDelayed(() -> {
                if (isAdded()) {
                    isScanning = true;
                    viewModel.setStatusMessage(null);
                }
            }, 3000); // 3 second delay
        }
    }

    private void toggleFlash() {
        if (barcodeView != null) {
            isFlashOn = !isFlashOn;
            if (isFlashOn) {
                barcodeView.setTorchOn();
            } else {
                barcodeView.setTorchOff();
            }
            binding.buttonToggleFlash.setText(isFlashOn ? "Flash Off" : "Flash On");
        }
    }

    private void closeScanner() {
        if (getActivity() != null) {
            getActivity().onBackPressed(); // Go back to previous fragment
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                viewModel.setStatusMessage("Camera permission is required to scan QR codes");
                binding.buttonClose.setVisibility(View.VISIBLE);
            }
        }
    }

    private void startScanning() {
        if (barcodeView != null) {
            barcodeView.resume();
            isScanning = true;
            viewModel.setStatusMessage(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (barcodeView != null && checkCameraPermission()) {
            barcodeView.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (barcodeView != null) {
            barcodeView.pause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (barcodeView != null) {
            barcodeView.pause();
        }
        binding = null;
    }
}
