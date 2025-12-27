import { useCallback, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { db } from '../constants/db';
import { getInkCanvasState, saveInkCanvasState, saveTextFieldsState } from '../services/inkCanvas';
import type { InkEditorRef } from '../components/InkEditor';

const QUERY_KEY_PREFIX = 'inkCanvasState';

export function useInkCanvasPersistence(canvasId: string) {
  const canvasRef = useRef<InkEditorRef>(null);
  const queryClient = useQueryClient();

  // Query to load initial state (strokes and text fields)
  const { data, isLoading, error } = useQuery({
    queryKey: [QUERY_KEY_PREFIX, canvasId],
    queryFn: async () => {
      const state = await getInkCanvasState(db, canvasId);
      return {
        strokesJson: state?.strokesJson ?? '[]',
        textFieldsJson: state?.textFieldsJson ?? '[]',
      };
    },
    staleTime: Infinity, // Don't refetch unless explicitly invalidated
  });

  // Mutation to save strokes
  const saveStrokesMutation = useMutation({
    mutationFn: async (strokesJson: string) => {
      await saveInkCanvasState(db, canvasId, strokesJson);
    },
    onSuccess: (_data, strokesJson) => {
      // Update the cache directly instead of refetching
      queryClient.setQueryData([QUERY_KEY_PREFIX, canvasId], (old: { strokesJson: string; textFieldsJson: string } | undefined) => ({
        strokesJson,
        textFieldsJson: old?.textFieldsJson ?? '[]',
      }));
    },
  });

  // Mutation to save text fields
  const saveTextFieldsMutation = useMutation({
    mutationFn: async (textFieldsJson: string) => {
      await saveTextFieldsState(db, canvasId, textFieldsJson);
    },
    onSuccess: (_data, textFieldsJson) => {
      // Update the cache directly instead of refetching
      queryClient.setQueryData([QUERY_KEY_PREFIX, canvasId], (old: { strokesJson: string; textFieldsJson: string } | undefined) => ({
        strokesJson: old?.strokesJson ?? '[]',
        textFieldsJson,
      }));
    },
  });

  // Debounced save handler for strokes
  const saveStrokesTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleStrokesChange = useCallback(
    (strokesJson: string) => {
      // Clear any pending save
      if (saveStrokesTimeoutRef.current) {
        clearTimeout(saveStrokesTimeoutRef.current);
      }

      // Debounce the save by 500ms
      saveStrokesTimeoutRef.current = setTimeout(() => {
        saveStrokesMutation.mutate(strokesJson);
      }, 500);
    },
    [saveStrokesMutation],
  );

  // Debounced save handler for text fields
  const saveTextFieldsTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleTextFieldsChange = useCallback(
    (textFieldsJson: string) => {
      // Clear any pending save
      if (saveTextFieldsTimeoutRef.current) {
        clearTimeout(saveTextFieldsTimeoutRef.current);
      }

      // Debounce the save by 500ms
      saveTextFieldsTimeoutRef.current = setTimeout(() => {
        saveTextFieldsMutation.mutate(textFieldsJson);
      }, 500);
    },
    [saveTextFieldsMutation],
  );

  return {
    canvasRef,
    initialStrokes: data?.strokesJson,
    initialTextFields: data?.textFieldsJson,
    isLoading,
    error,
    handleStrokesChange,
    handleTextFieldsChange,
    isSaving: saveStrokesMutation.isPending || saveTextFieldsMutation.isPending,
  };
}
