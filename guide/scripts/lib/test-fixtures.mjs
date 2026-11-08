// Shared fixtures for the node:test self-tests. Not a test file itself.

// A package.json dependency section equal to the allowlist, as a fresh object.
export function goodPackage() {
  return {
    dependencies: {
      '@docusaurus/core': '3.10.2',
      '@docusaurus/faster': '3.10.2',
      '@docusaurus/preset-classic': '3.10.2',
      '@mdx-js/react': '^3.0.0',
      clsx: '^2.0.0',
      'prism-react-renderer': '^2.3.0',
      react: '^19.0.0',
      'react-dom': '^19.0.0',
    },
    devDependencies: {
      '@docusaurus/module-type-aliases': '3.10.2',
      '@docusaurus/tsconfig': '3.10.2',
      '@docusaurus/types': '3.10.2',
      '@types/react': '^19.0.0',
      typescript: '~6.0.2',
    },
  };
}
