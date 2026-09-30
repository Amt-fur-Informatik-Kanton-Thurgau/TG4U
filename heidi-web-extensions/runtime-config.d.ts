/**
 * Configuration this extension needs on top of the cockpit's own. Deployments
 * supply these in the `config.json` they mount, alongside the built-in keys.
 */
import "@/lib/runtime-config";

declare module "@/lib/runtime-config" {
  interface RuntimeConfigExtensions {
    /** Public issuer service URL used for hosted static issuance. */
    heidiIssuerBaseUrl: string;
  }
}
