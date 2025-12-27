---
applyTo: 'apps/drawmark/src/**/*'
---

This project is using React Native with TypeScript. Please follow the instructions below to set up your development environment and get started with the project.

## General Guidance

- Do not use emoji in the UI since it cheapens the user experience. Instead, use proper icons from `@react-native-vector-icons/ionicons` (RNVI Ionicons).
  - Docs for RNVI Ionicons: https://www.npmjs.com/package/@react-native-vector-icons/ionicons
- Do not use inline styles or StyleSheet for any styling. Instead, use `className` and Tailwind classnames. We have a transformer that converts Tailwind classnames to React Native styles.
- Use TanStack Query for data fetching and state management.
- Use Drizzle ORM for database interactions.
- Use React Navigation for navigation.
- Never use the built-in `Button` component
- While making DB schema files is important, never manually make `Drizzle` migration files (including the `_journal.json` file). Instead, use `pnpm gen:db` to generate migration files automatically.

## Folder structure

- `src/` - Contains all the source code for the application.
  - `components/` - Application-wide reusable UI components.
  - `constants/` - Application-wide constants and configurations.
  - `hooks/` - Application-wide custom React hooks to compose services and more.
  - `services/` - Application-wide pure JS async services and business logic; no React code or imports.
  - `utils/` - Pure JS sync code; Utility functions and helpers.
  - `types/` - Application-wide type definitions.
  - `views/` - Screen components for different app views.
    - `SCREEN_NAME/` - Each screen has its own folder.
      - `SCREEN_NAME.view.tsx` - Business logic for the screen.
      - `SCREEN_NAME.ui.tsx` - UI and JSX specific to the screen; should only have presentational code and one component.
      - `SCREEN_NAME.styles.ts` - Styles for the screen using StyleSheet.
      - `types/` - Screen-specific type definitions.
      - `hooks/` - Screen-specific custom hooks.
      - `utils/` - Screen-specific pure JS sync code.
      - `constants/` - Screen-specific constants and configurations.
      - `services/` - Screen-specific pure JS async services and business logic.
      - `components/` - Screen-specific components; all presentational.
        - `COMPONENT_NAME/` - Each component has its own folder.
          - `COMPONENT_NAME.ui.tsx` - UI and JSX for the component.
          - `COMPONENT_NAME.styles.ts` - Styles for the component.

If you need help understanding the folder structure, refer to the [Layered React Structure (LRS) guide](https://playfulprogramming.com/posts/layered-react-structure)

## Commands

- `npm install` - Install all project dependencies.
- `npm run android` - Run the app on an Android device or emulator.
- `npm run lint` - Run ESLint to check for code quality issues.
- `npm run test` - Run the test suite using Jest.
