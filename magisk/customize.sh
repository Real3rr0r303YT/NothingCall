# NothingCall system mode – Magisk install script
ui_print "- NothingCall: installing as privileged system app"
if [ ! -f "$MODPATH/system/priv-app/NothingCall/NothingCall.apk" ]; then
  abort "! NothingCall.apk missing – use the module ZIP from the GitHub release / CI artifacts"
fi
set_perm_recursive "$MODPATH/system" 0 0 0755 0644
ui_print "- Reboot, then open NothingCall → Settings → System mode should show 'Active'"
