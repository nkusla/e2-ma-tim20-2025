package com.kulenina.questix.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.kulenina.questix.R;
import com.kulenina.questix.databinding.FragmentQuestsBinding;
import com.kulenina.questix.viewmodel.QuestsViewModel;

public class QuestsFragment extends Fragment {
    private FragmentQuestsBinding binding;
    private QuestsViewModel questsViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_quests, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        questsViewModel = new QuestsViewModel();
        binding.setViewModel(questsViewModel);
    }
}
