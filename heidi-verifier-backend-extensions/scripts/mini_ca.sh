#!/bin/bash
#
# Builds a throw-away issuing chain for local experimentation: a self-signed
# authority and an issuer certificate under it, both in DER form.
#
# The keys are generated here rather than kept beside this script, so no private
# key is ever committed. Delete the generated files to start over; everything
# this produces is disposable and must never be trusted outside local testing.

set -Eeuo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

[[ -f authority.key ]] || openssl ecparam -name prime256v1 -genkey -noout -out authority.key
[[ -f issuer.key ]] || openssl ecparam -name prime256v1 -genkey -noout -out issuer.key

openssl req \
	-addext basicConstraints=CA:TRUE \
	-outform der -out authority.der \
	-key authority.key \
	-subj "/CN=Heidi Local Test Authority" \
	-new -x509 \
	-days 365

openssl req \
	-addext basicConstraints=CA:TRUE \
	-outform der -out issuer.der \
	-CA authority.der -CAkey authority.key \
	-key issuer.key \
	-subj "/CN=Heidi Local Test Issuer" \
	-new -x509 \
	-days 365 \
	-set_serial 01

echo "Wrote authority.der and issuer.der (keys: authority.key, issuer.key)"
