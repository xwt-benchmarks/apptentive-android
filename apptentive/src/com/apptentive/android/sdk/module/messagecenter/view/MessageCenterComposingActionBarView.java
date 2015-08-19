/*
 * Copyright (c) 2015, Apptentive, Inc. All Rights Reserved.
 * Please refer to the LICENSE file for the terms and conditions
 * under which redistribution and use of this file is permitted.
 */

package com.apptentive.android.sdk.module.messagecenter.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;

import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;

import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;


import com.apptentive.android.sdk.R;
import com.apptentive.android.sdk.module.messagecenter.model.MessageCenterComposingItem;


/**
 * @author Barry Li
 */
public class MessageCenterComposingActionBarView extends FrameLayout
		implements MessageCenterListItemView {


	public MessageCenterComposingActionBarView(final Context context, final MessageCenterComposingItem item,
																						 final MessageAdapter.OnComposingActionListener listener) {
		super(context);

		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.apptentive_message_center_composing_actionbar, this);

		View closeButton = findViewById(R.id.cancel_composing);
		closeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				CloseConfirmationDialog editNameDialog = new CloseConfirmationDialog();
				editNameDialog.listener = listener;
				Bundle bundle = new Bundle();
				bundle.putString("STR_2", item.str_2);
				bundle.putString("STR_3", item.str_3);
				bundle.putString("STR_4", item.str_4);
				editNameDialog.setArguments(bundle);
				editNameDialog.show(((Activity) context).getFragmentManager(), "CloseConfirmationDialog");
			}
		});

		TextView composing = (TextView) findViewById(R.id.composing);

		if (item.str_1 != null) {
			composing.setText(item.str_1);
		}

		ImageButton sendButton = (ImageButton) findViewById(R.id.btn_send_message);
		if (item.button_1 != null) {
			sendButton.setContentDescription(item.button_1);
		}
		sendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				listener.onFinishComposing();
			}
		});

	}

	public static class CloseConfirmationDialog extends DialogFragment {
		public MessageAdapter.OnComposingActionListener listener;

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new AlertDialog.Builder(getActivity())
					.setMessage(getArguments().getString("STR_2"))
					.setPositiveButton(getArguments().getString("STR_3"),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									listener.onCancelComposing();
									dialog.dismiss();
								}
							})
					.setNegativeButton(getArguments().getString("STR_4"),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							}).create();
		}

	}

}