package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.PhoneVerificationManager
import com.example.auth.PhoneVerificationStage
import com.example.auth.PhoneVerificationUiState
import com.example.integrations.BusinessLineId
import com.example.integrations.BusinessPhonesConfig
import com.example.ui.theme.OceanTheme

@Composable
fun PhoneVerificationCard(
  businessPhonesConfig: BusinessPhonesConfig,
  verificationState: PhoneVerificationUiState,
  onSelectLine: (BusinessLineId) -> Unit,
  onSendFirebaseSms: (BusinessLineId, String) -> Unit,
  onSendTwoPhonePin: (BusinessLineId, String) -> Unit,
  onVerifyCode: (String) -> Unit,
  onResetVerification: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val colors = OceanTheme.colors
  val telephonyStatus = remember(context) { PhoneVerificationManager.readDeviceTelephonyStatus(context) }

  val targetLine = verificationState.targetLine
  val defaultNumber = if (targetLine == BusinessLineId.PHONE_2) {
    businessPhonesConfig.phone2Number
  } else {
    businessPhonesConfig.phone1Number
  }

  var phoneNumberInput by remember(targetLine, defaultNumber) { mutableStateOf(defaultNumber) }
  var otpCodeInput by remember(verificationState.stage) { mutableStateOf("") }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("phone_verification_card"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
    border = BorderStroke(
      1.dp,
      if (businessPhonesConfig.isLineVerified(targetLine)) colors.seaGreen.copy(alpha = 0.65f)
      else colors.goldenSun.copy(alpha = 0.55f)
    )
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = CircleShape,
          color = colors.goldenSun.copy(alpha = 0.2f),
          modifier = Modifier.size(40.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.PhonelinkLock,
              contentDescription = "Phone Verification",
              tint = colors.goldenSun,
              modifier = Modifier.size(22.dp)
            )
          }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Phone Number Verification",
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
          Text(
            text = "Verify Phone 1 & Phone 2 via Firebase SMS OTP or 2-Phone SMS PIN",
            color = colors.cyan,
            fontSize = 11.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // SIM & Verification Badges Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(BusinessLineId.PHONE_1, BusinessLineId.PHONE_2).forEach { line ->
          val isSelected = targetLine == line
          val isVerified = businessPhonesConfig.isLineVerified(line)
          FilterChip(
            selected = isSelected,
            onClick = { onSelectLine(line) },
            leadingIcon = if (isVerified) {
              {
                Icon(
                  imageVector = Icons.Default.Verified,
                  contentDescription = "Verified",
                  tint = if (isSelected) colors.abyss else colors.seaGreen,
                  modifier = Modifier.size(14.dp)
                )
              }
            } else null,
            label = {
              Text(
                text = "${line.shortLabel}${if (isVerified) " ✓" else ""}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = colors.goldenSun,
              selectedLabelColor = colors.abyss,
              containerColor = colors.card,
              labelColor = colors.textPrimary
            ),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 44.dp)
              .testTag("verify_line_chip_${line.id.lowercase()}")
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // SIM Carrier Telemetry Banner (Zero sensitive/prohibited permissions)
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.card,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.SimCard,
            contentDescription = null,
            tint = if (telephonyStatus.simReady) colors.seaGreen else colors.cyan,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          val simText = when {
            !telephonyStatus.hasReadPhoneState -> "SIM status: Grant phone state permission in Step 3 to read carrier"
            telephonyStatus.simReady && telephonyStatus.carrierName.isNotBlank() ->
              "Device SIM Active • Carrier: ${telephonyStatus.carrierName}"
            telephonyStatus.simReady -> "Device SIM Active & Ready"
            else -> "No physical SIM detected on this device (SMS OTP still works to any cell)"
          }
          Text(
            text = simText,
            color = colors.textSecondary,
            fontSize = 11.sp
          )
        }
      }

      if (businessPhonesConfig.isLineVerified(targetLine)) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = colors.seaGreen.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, colors.seaGreen.copy(alpha = 0.6f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("phone_verified_badge_${targetLine.id.lowercase()}")
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Verified",
              tint = colors.seaGreen,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${targetLine.shortLabel} Verified (${businessPhonesConfig.verifiedMethod.ifBlank { "SMS" }})",
                color = colors.seaGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              val verifiedNum = if (targetLine == BusinessLineId.PHONE_2) {
                businessPhonesConfig.phone2Number
              } else {
                businessPhonesConfig.phone1Number
              }
              if (verifiedNum.isNotBlank()) {
                Text(
                  text = verifiedNum,
                  color = colors.textPrimary,
                  fontSize = 11.sp
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = phoneNumberInput,
        onValueChange = { phoneNumberInput = it },
        label = { Text("${targetLine.shortLabel} Cell Number (e.g. +1 808-555-1234)", fontSize = 11.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Phone,
          autoCorrectEnabled = false
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("phone_verify_number_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = colors.goldenSun,
          unfocusedBorderColor = colors.teal,
          focusedTextColor = colors.textPrimary,
          unfocusedTextColor = colors.textPrimary
        )
      )

      Spacer(modifier = Modifier.height(10.dp))

      when (val stage = verificationState.stage) {
        is PhoneVerificationStage.Error -> {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.coralAlert.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, colors.coralAlert.copy(alpha = 0.6f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 10.dp)
              .testTag("phone_verify_error_banner")
          ) {
            Text(
              text = stage.message,
              color = colors.textPrimary,
              fontSize = 11.sp,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
        is PhoneVerificationStage.SendingCode, PhoneVerificationStage.Verifying -> {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 10.dp)
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = colors.goldenSun,
              strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (stage is PhoneVerificationStage.SendingCode) {
                "Sending SMS verification to ${stage.phoneNumber}..."
              } else {
                "Verifying 6-digit code..."
              },
              color = colors.goldenSun,
              fontSize = 11.sp
            )
          }
        }
        is PhoneVerificationStage.CodeSent -> {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.turquoise.copy(alpha = 0.55f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 10.dp)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = stage.message,
                color = colors.turquoise,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = otpCodeInput,
                  onValueChange = { raw ->
                    val digits = raw.filter { ch -> ch.isDigit() }
                    if (digits.length <= 6) otpCodeInput = digits
                  },
                  label = { Text("6-Digit Code", fontSize = 11.sp) },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    autoCorrectEnabled = false
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("phone_verify_otp_input"),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.turquoise,
                    unfocusedBorderColor = colors.teal,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                  )
                )
                Button(
                  onClick = { onVerifyCode(otpCodeInput) },
                  colors = ButtonDefaults.buttonColors(containerColor = colors.seaGreen),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("verify_phone_code_button")
                ) {
                  Text("Verify", color = colors.abyss, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
              }
              Spacer(modifier = Modifier.height(6.dp))
              OutlinedButton(
                onClick = onResetVerification,
                border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("Cancel / Resend Code", color = colors.textSecondary, fontSize = 11.sp)
              }
            }
          }
        }
        else -> Unit
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = { onSendFirebaseSms(targetLine, phoneNumberInput) },
          colors = ButtonDefaults.buttonColors(containerColor = colors.goldenSun),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 48.dp)
            .testTag("send_firebase_sms_button")
        ) {
          Icon(
            imageVector = Icons.Default.Sms,
            contentDescription = null,
            tint = colors.abyss,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Send SMS OTP", color = colors.abyss, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        OutlinedButton(
          onClick = { onSendTwoPhonePin(targetLine, phoneNumberInput) },
          border = BorderStroke(1.dp, colors.turquoise),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 48.dp)
            .testTag("send_two_phone_pin_button")
        ) {
          Text("Verify via 2-Phone PIN", color = colors.turquoise, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
      }
    }
  }
}
