import argparse
import json
import hashlib
import base64
from jwcrypto import jwk
from jwcrypto import jwt

parser = argparse.ArgumentParser(
    description="Utility script to generate signing keypairs.",
    epilog="As the script currently only supports EC keys, no arguments are needed.",
)


def generate_keypair() -> jwk.JWK:
    return jwk.JWK.generate(kty="EC", alg="ES256", kid="0")


keypair = generate_keypair()

print("===== Private key =====")
print(keypair.export_private())
print()
print("===== Public key ======")
print(keypair.export_public())
